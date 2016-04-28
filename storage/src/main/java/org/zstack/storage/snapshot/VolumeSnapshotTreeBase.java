package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus.StatusEvent;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.backup.BackupStorageCapacityUpdater;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.message.OperationChecker;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotTreeBase {
    private static CLogger logger = Utils.getLogger(VolumeSnapshotTreeBase.class);

    protected VolumeSnapshotVO currentRoot;
    protected SnapshotLeaf currentLeaf;
    protected VolumeSnapshotTree fullTree;

    protected String syncSignature;

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CascadeFacade casf;
    protected static OperationChecker allowedStatus = new OperationChecker(true);

    static {
        allowedStatus.addState(VolumeSnapshotStatus.Ready,
                VolumeSnapshotDeletionMsg.class.getName(),
                BackupVolumeSnapshotMsg.class.getName(),
                CreateDataVolumeFromVolumeSnapshotMsg.class.getName(),
                CreateTemplateFromVolumeSnapshotMsg.class.getName(),
                VolumeSnapshotBackupStorageDeletionMsg.class.getName(),
                APIRevertVolumeFromSnapshotMsg.class.getName(),
                APIDeleteVolumeSnapshotFromBackupStorageMsg.class.getName(),
                APIBackupVolumeSnapshotMsg.class.getName()
        );

        allowedStatus.addState(VolumeSnapshotStatus.Deleting,
                VolumeSnapshotDeletionMsg.class.getName());

        allowedStatus.addState(VolumeSnapshotStatus.Creating,
                VolumeSnapshotDeletionMsg.class.getName());
    }

    private ErrorCode isOperationAllowed(Message msg) {
        if (allowedStatus.isOperationAllowed(msg.getClass().getName(), currentRoot.getStatus().toString())) {
            return null;
        } else {
            return errf.instantiateErrorCode(VolumeSnapshotErrors.NOT_IN_CORRECT_STATE,
                    String.format("snapshot[uuid:%s, name:%s]'s status[%s] is not allowed for message[%s], allowed status%s",
                            currentRoot.getUuid(), currentRoot.getName(), currentRoot.getStatus(), msg.getClass().getName(), allowedStatus.getStatesForOperation(msg.getClass().getName())));
        }
    }

    private void refreshVO()  {
        VolumeSnapshotVO vo = dbf.reload(currentRoot);
        if (vo == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("cannot find volume snapshot[uuid:%s, name:%s], it may have been deleted by previous operation",
                    currentRoot.getUuid(), currentRoot.getName())));
        }

        currentRoot = vo;
        buildFullSnapshotTree();
        currentLeaf = fullTree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getUuid().equals(currentRoot.getUuid());
            }
        });
    }

    private VolumeSnapshotInventory getSelfInventory() {
        return VolumeSnapshotInventory.valueOf(currentRoot);
    }

    private void buildFullSnapshotTree() {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, SimpleQuery.Op.EQ, currentRoot.getTreeUuid());
        List<VolumeSnapshotVO> vos = q.list();

        fullTree = VolumeSnapshotTree.fromVOs(vos);
    }

    public VolumeSnapshotTreeBase(VolumeSnapshotVO vo, boolean syncOnVolume) {
        currentRoot = vo;
        if (syncOnVolume) {
            syncSignature = String.format("volume.snapshot.volume.%s", currentRoot.getVolumeUuid());
        } else {
            syncSignature = String.format("volume.snapshot.tree.%s", currentRoot.getTreeUuid());
        }
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateTemplateFromVolumeSnapshotMsg) {
            handle((CreateTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateDataVolumeFromVolumeSnapshotMsg) {
            handle((CreateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotDeletionMsg) {
            handle((VolumeSnapshotDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final VolumeSnapshotDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deletion(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void deletion(final VolumeSnapshotDeletionMsg msg, final NoErrorCompletion completion) {
        final VolumeSnapshotDeletionReply reply = new VolumeSnapshotDeletionReply();

        try {
            refreshVO();
        } catch (OperationFailureException e) {
            // the snapshot has been deleted
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-snapshot-%s", currentRoot.getUuid()));
        chain.allowEmptyFlow();

        boolean ancestorOfLatest = false;
        for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
            if (inv.isLatest()) {
                ancestorOfLatest =  true;
                break;
            }
        }

        chain.then(new Flow() {
            String __name__ = String.format("change-volume-snapshot-status-%s", VolumeSnapshotStatus.Deleting);

            public void run(final FlowTrigger trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.delete, currentLeaf.getDescendants(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(final FlowRollback trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        });

        if (!msg.isVolumeDeletion()) {
            // this deletion is caused by snapshot deletion, check if merge need
            SimpleQuery<VolumeSnapshotTreeVO> tq  = dbf.createQuery(VolumeSnapshotTreeVO.class);
            tq.select(VolumeSnapshotTreeVO_.current);
            tq.add(VolumeSnapshotTreeVO_.uuid, Op.EQ, currentRoot.getTreeUuid());
            Boolean onCurrentTree = tq.findValue();

            boolean needMerge = onCurrentTree && ancestorOfLatest && currentRoot.getPrimaryStorageUuid() != null && VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString().equals(currentRoot.getType());
            if (needMerge) {
                chain.then(new NoRollbackFlow() {
                    String __name__ = "merge-volume-snapshots-to-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        MergeVolumeSnapshotOnPrimaryStorageMsg mmsg = new MergeVolumeSnapshotOnPrimaryStorageMsg();
                        VolumeSnapshotInventory from = currentLeaf.getParent() == null ? currentLeaf.getInventory() : currentLeaf.getParent().getInventory();
                        mmsg.setFrom(from);
                        VolumeVO vol = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
                        mmsg.setTo(VolumeInventory.valueOf(vol));
                        mmsg.setFullRebase(currentLeaf.getParent() == null);
                        bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(mmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });
            }

            chain.then(new NoRollbackFlow() {
                String __name__ = "delete-volume-snapshots-from-backup-storage";

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    final List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = makeVolumeSnapshotBackupStorageDeletionMsg(null);
                    if (dmsgs.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                        @Override
                        public void run(List<MessageReply> replies) {
                            for (MessageReply r : replies) {
                                if (!r.isSuccess()) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = dmsgs.get(replies.indexOf(r));
                                    logger.warn(String.format("failed to delete snapshot[uuid:%s] on backup storage[uuids: %s], the backup storage should cleanup",
                                            dmsg.getSnapshotUuid(), dmsg.getBackupStorageUuids()));
                                }
                            }

                            trigger.next();
                        }
                    });
                }
            });
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-snapshots-from-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final List<VolumeSnapshotPrimaryStorageDeletionMsg> pmsgs = CollectionUtils.transformToList(currentLeaf.getDescendants(), new Function<VolumeSnapshotPrimaryStorageDeletionMsg, VolumeSnapshotInventory>() {
                    @Override
                    public VolumeSnapshotPrimaryStorageDeletionMsg call(VolumeSnapshotInventory arg) {
                        if (arg.getPrimaryStorageUuid() == null) {
                            return null;
                        }

                        VolumeSnapshotPrimaryStorageDeletionMsg pmsg = new VolumeSnapshotPrimaryStorageDeletionMsg();
                        pmsg.setUuid(arg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(pmsg, VolumeSnapshotConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                        return pmsg;
                    }
                });

                if (pmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(pmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                VolumeSnapshotPrimaryStorageDeletionMsg pmsg = pmsgs.get(replies.indexOf(r));
                                logger.warn(String.format("failed to delete snapshot[uuid:%s] on primary storage[uuid:%s], the primary storage should cleanup",
                                        pmsg.getSnapshotUuid(), currentRoot.getPrimaryStorageUuid()));
                            }
                        }

                        trigger.next();
                    }
                });
            }
        });

        final boolean finalAncestorOfLatest = ancestorOfLatest;
        chain.done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                if (msg.isVolumeDeletion()) {
                    new Runnable() {
                        @Override
                        @Transactional
                        public void run() {
                            String sql = "update VolumeSnapshotTreeVO tree set tree.volumeUuid = NULL where tree.volumeUuid = :volUuid";
                            Query q = dbf.getEntityManager().createQuery(sql);
                            q.setParameter("volUuid", currentRoot.getVolumeUuid());
                            q.executeUpdate();

                            sql = "update VolumeSnapshotVO s set s.volumeUuid = NULL where s.volumeUuid = :volUuid";
                            q = dbf.getEntityManager().createQuery(sql);
                            q.setParameter("volUuid", currentRoot.getVolumeUuid());
                            q.executeUpdate();
                        }
                    }.run();
                }

                if (!msg.isVolumeDeletion() && finalAncestorOfLatest && currentRoot.getParentUuid() != null) {
                    // reset latest
                    VolumeSnapshotVO vo = dbf.findByUuid(currentRoot.getParentUuid(), VolumeSnapshotVO.class);
                    vo.setLatest(true);
                    dbf.update(vo);
                    logger.debug(String.format("reset latest snapshot of tree[uuid:%s] to snapshot[uuid:%s]",
                            currentRoot.getTreeUuid(), currentRoot.getParentUuid()));
                }


                if (!cleanup()) {
                    changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(msg, completion) {
                        @Override
                        public void success() {
                            bus.reply(msg, reply);
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            completion.done();
                        }
                    });
                } else {
                    bus.reply(msg, reply);
                    completion.done();
                }
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();
    }

    private void handle(final CreateDataVolumeFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createDataVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void createDataVolume(final CreateDataVolumeFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final CreateDataVolumeFromVolumeSnapshotReply reply = new CreateDataVolumeFromVolumeSnapshotReply();

        refreshVO();
        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        final CreateBitsFromSnapshotInfoForDataVolume info = new CreateBitsFromSnapshotInfoForDataVolume(prepareCreateBitsFromSnapshotInfo(msg.getPrimaryStorageUuid()));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (info.needDownload) {
                    flow(new Flow() {
                        String __name__ = "allocateHost-workspace-primary-storage";
                        boolean success;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setRequiredPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            amsg.setSize(info.neededSizeOnWorkspacePrimaryStorage);
                            amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadSnapshot.toString());
                            amsg.setNoOverProvisioning(true);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        success = true;
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (success) {
                                ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                rmsg.setDiskSize(info.neededSizeOnWorkspacePrimaryStorage);
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "create-data-volume-on-primary-storage";

                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg();
                        cmsg.setNeedDownload(info.needDownload);
                        cmsg.setSnapshots(info.snapshotDownloadInfos);
                        cmsg.setVolumeUuid(msg.getVolume().getUuid());
                        cmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply cr = reply.castReply();
                                    info.bitsInstallPath = cr.getInstallPath();
                                    info.bitsSize = cr.getSize();
                                    info.bitsActualSize = cr.getActualSize();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (info.bitsInstallPath != null) {
                            DeleteBitsOnPrimaryStorageMsg dmsg = new DeleteBitsOnPrimaryStorageMsg();
                            dmsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(getSelfInventory().getFormat()).toString());
                            dmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            dmsg.setInstallPath(info.bitsInstallPath);
                            dmsg.setBitsType(VolumeVO.class.getSimpleName());
                            dmsg.setBitsUuid(msg.getVolumeUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        trigger.rollback();
                    }
                });

                if (info.needDownload) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-workspace-primary-storage-capacity";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            rmsg.setDiskSize(info.totalSnapshotSize);
                            rmsg.setNoOverProvisioning(true);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                            bus.send(rmsg);
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory inv = msg.getVolume();
                        inv.setInstallPath(info.bitsInstallPath);
                        inv.setSize(info.bitsSize);
                        inv.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                        inv.setFormat(currentRoot.getFormat());
                        reply.setActualSize(info.bitsActualSize);
                        reply.setInventory(inv);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final CreateTemplateFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createTemplate(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-template-from-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void changeStatusOfSnapshots(final StatusEvent evt, final List<VolumeSnapshotInventory> snapshots, final Completion completion) {
        List<ChangeVolumeSnapshotStatusMsg> msgs = CollectionUtils.transformToList(snapshots, new Function<ChangeVolumeSnapshotStatusMsg, VolumeSnapshotInventory>() {
            @Override
            public ChangeVolumeSnapshotStatusMsg call(VolumeSnapshotInventory arg) {
                ChangeVolumeSnapshotStatusMsg msg = new ChangeVolumeSnapshotStatusMsg();
                msg.setEvent(evt.toString());
                msg.setSnapshotUuid(arg.getUuid());
                bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                ErrorCode err = null;
                VolumeSnapshotInventory failSnapshot = null;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        err = r.getError();
                        failSnapshot = snapshots.get(replies.indexOf(r));
                        break;
                    }
                }

                if (err != null) {
                    completion.fail(errf.instantiateErrorCode(
                            SysErrors.OPERATION_ERROR,
                            String.format("failed to change status of volume snapshot[uuid:%s, name:%s] by status event[%s]",
                                    failSnapshot.getUuid(), failSnapshot.getName(), evt),
                            err
                    ));
                } else {
                    completion.success();
                }
            }
        });
    }

    class CreateBitsFromSnapshotInfo {
        List<VolumeSnapshotInventory> snapshots;
        PrimaryStorageInventory workspacePrimaryStorage;
        long totalSnapshotSize;
        long bitsSize;
        long bitsActualSize;
        List<String> zoneUuidsForFindingWorkspacePrimaryStorage;
    }

    class CreateBitsFromSnapshotInfoForTemplate extends  CreateBitsFromSnapshotInfo {
        CreateBitsFromSnapshotInfoForTemplate(CreateBitsFromSnapshotInfo info) {
            workspacePrimaryStorage = info.workspacePrimaryStorage;
            totalSnapshotSize = info.totalSnapshotSize;
            bitsSize = info.bitsSize;
            zoneUuidsForFindingWorkspacePrimaryStorage = info.zoneUuidsForFindingWorkspacePrimaryStorage;
        }

        List<BackupStorageInventory> destBackupStorages = new ArrayList<BackupStorageInventory>();
        List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();
    }

    class CreateBitsFromSnapshotInfoForDataVolume extends CreateBitsFromSnapshotInfo {
        String bitsInstallPath;

        CreateBitsFromSnapshotInfoForDataVolume(CreateBitsFromSnapshotInfo info) {
            workspacePrimaryStorage = info.workspacePrimaryStorage;
            totalSnapshotSize = info.totalSnapshotSize;
            bitsSize = info.bitsSize;
            zoneUuidsForFindingWorkspacePrimaryStorage = info.zoneUuidsForFindingWorkspacePrimaryStorage;
        }
    }

    private CreateBitsFromSnapshotInfo prepareCreateBitsFromSnapshotInfo(final String requiredPrimaryStorage) {
        final CreateBitsFromSnapshotInfo info = new CreateBitsFromSnapshotInfo();
        final List<VolumeSnapshotInventory> ancestors = currentLeaf.getAncestors();

        for (VolumeSnapshotInventory inv : ancestors) {
            if (inv.getPrimaryStorageUuid() == null) {
                throw new CloudRuntimeException(String.format("the volume snapshot[uuid:%s, name:%s] is not on any primary storage", inv.getUuid() ,inv.getName()));
            }

            info.totalSnapshotSize += inv.getSize();
        }

        info.snapshots = new ArrayList<VolumeSnapshotInventory>();
        for (VolumeSnapshotInventory inv : ancestors) {
            info.snapshots.add(inv);
        }

        PrimaryStorageVO privo = dbf.findByUuid(currentRoot.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        info.workspacePrimaryStorage  = PrimaryStorageInventory.valueOf(privo);

        return info;
    }

    private void createTemplate(final CreateTemplateFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final CreateTemplateFromVolumeSnapshotReply reply = new CreateTemplateFromVolumeSnapshotReply();

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }


        final CreateBitsFromSnapshotInfoForTemplate info = new CreateBitsFromSnapshotInfoForTemplate(prepareCreateBitsFromSnapshotInfo(null));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            String temporaryInstallPath;
            long size;
            long actualSize;
            List<String> targetBackupStorageUuids = new ArrayList<String>();
            List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();

            @Override
            public void setup() {
                /*
                flow(new Flow() {
                    String __name__ = "allocateHost-destination-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msg.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                            amsg.setSize(info.totalSnapshotSize);
                            amsg.setRequiredZoneUuid(info.workspacePrimaryStorage.getZoneUuid());
                            bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        AllocateBackupStorageReply ar = reply.castReply();
                                        info.destBackupStorages.add(ar.getInventory());
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msg.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                                    amsg.setBackupStorageUuid(arg);
                                    amsg.setSize(info.totalSnapshotSize);
                                    amsg.setRequiredZoneUuid(info.workspacePrimaryStorage.getZoneUuid());
                                    bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                                    return amsg;
                                }
                            });

                            bus.send(amsgs, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    List<ErrorCode> errs = new ArrayList<ErrorCode>();
                                    for (MessageReply r : replies) {
                                        String bsUuid = msg.getBackupStorageUuids().get(replies.indexOf(r));
                                        if (!r.isSuccess()) {
                                            errs.add(r.getError());
                                            logger.warn(String.format("failed to allocate backup storage[uuid:%s] because %s",
                                                    bsUuid, r.getError()));
                                        } else {
                                            info.destBackupStorages.add(((AllocateBackupStorageReply)r).getInventory());
                                        }
                                    }

                                    if (info.destBackupStorages.isEmpty()) {
                                        trigger.fail(errf.stringToOperationError(String.format("failed to allocate all backup storage[uuids:%s], a list of error: %s",
                                                msg.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs))));
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!info.destBackupStorages.isEmpty()) {
                            List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(info.destBackupStorages, new Function<ReturnBackupStorageMsg, BackupStorageInventory>() {
                                @Override
                                public ReturnBackupStorageMsg call(BackupStorageInventory arg) {
                                    ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                    rmsg.setBackupStorageUuid(arg.getUuid());
                                    rmsg.setSize(info.totalSnapshotSize);
                                    bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, arg.getUuid());
                                    return rmsg;
                                }
                            });

                            bus.send(rmsgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            BackupStorageInventory bs = info.destBackupStorages.get(replies.indexOf(r));
                                            logger.warn(String.format("failed to return capacity[%s bytes] to backup storage[uuid:%s], because %s",
                                                    info.totalSnapshotSize, bs.getUuid(), r.getError()));
                                        }
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });
                */

                flow(new Flow() {
                    String __name__ = "create-template-from-volume-snapshot-on-primary-storage";

                    private void returnBackupStorageForFailure(CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply) {


                        // failed to upload to some backup storage, return capacity to them
                        List<String> successBsUuids = CollectionUtils.transformToList(reply.getResults(), new Function<String, CreateTemplateFromVolumeSnapshotResult>() {
                            @Override
                            public String call(CreateTemplateFromVolumeSnapshotResult arg) {
                                return arg.getBackupStorageUuid();
                            }
                        });

                        List<String> allBsUuids = CollectionUtils.transformToList(info.destBackupStorages, new Function<String, BackupStorageInventory>() {
                            @Override
                            public String call(BackupStorageInventory arg) {
                                return arg.getUuid();
                            }
                        });

                        allBsUuids.removeAll(successBsUuids);

                        logger.debug(String.format("return capacity to backup storage%s, because they fails to upload image", allBsUuids));
                        List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(allBsUuids, new Function<ReturnBackupStorageMsg, String>() {
                            @Override
                            public ReturnBackupStorageMsg call(String arg) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(arg);
                                rmsg.setSize(info.totalSnapshotSize);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, arg);
                                return rmsg;
                            }
                        });

                        bus.send(rmsgs);
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg();
                        cmsg.setImageUuid(msg.getImageUuid());
                        cmsg.setBackupStorage(info.destBackupStorages);
                        cmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        cmsg.setSnapshots(info.snapshots);
                        cmsg.setCurrent(currentLeaf.getInventory());
                        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());

                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply cr = reply.castReply();
                                    actualSize = cr.getActualSize();
                                    size = cr.getSize();
                                    temporaryInstallPath = cr.getTemporaryInstallPath();

                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (temporaryInstallPath  != null) {
                            DeleteBitsOnPrimaryStorageMsg dmsg = new DeleteBitsOnPrimaryStorageMsg();
                            dmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                            dmsg.setInstallPath(temporaryInstallPath);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "reserve-storage-on-backup-storage";

                    @Transactional(readOnly = true)
                    private List<String> getCandidateBackupStorage() {
                        if (msg.getBackupStorageUuids() == null && !msg.getBackupStorageUuids().isEmpty()) {
                            String sql = "select bs.uuid from BackupStorageVO bs, BackupStorageZoneRefVO ref, PrimaryStorageVO pri" +
                                    " where bs.uuid = ref.backupStorageUuid and ref.zoneUuid = pri.zoneUuid and pri.uuid = :psUuid";
                            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                            q.setParameter("psUuid", currentRoot.getPrimaryStorageUuid());
                            return q.getResultList();
                        } else {
                            String sql = "select bs.uuid from BackupStorageVO bs, BackupStorageZoneRefVO ref, PrimaryStorageVO pri" +
                                    " where bs.uuid = ref.backupStorageUuid and ref.zoneUuid = pri.zoneUuid and pri.uuid = :psUuid" +
                                    " and bs.uuid in (:bsUuids)";
                            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                            q.setParameter("psUuid", currentRoot.getPrimaryStorageUuid());
                            q.setParameter("bsUuids", msg.getBackupStorageUuids());
                            List<String> bsUuids = q.getResultList();
                            if (!bsUuids.containsAll(msg.getBackupStorageUuids())) {
                                for (String bsUuid : msg.getBackupStorageUuids()) {
                                    if (!bsUuids.contains(bsUuid)) {
                                        //TODO
                                        logger.warn(String.format("the backup storage[uuid:%s] is not attached to the zone of the primary storage[uuid:%s] that" +
                                                " the snapshot[uuid:%s, name:%s] is on", bsUuid, currentRoot.getPrimaryStorageUuid(), currentRoot.getUuid(), currentRoot.getName()));
                                    }
                                }
                            }

                            return bsUuids;
                        }
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> bsUuids = getCandidateBackupStorage();
                        if (bsUuids.isEmpty()) {
                            throw new OperationFailureException(errf.stringToOperationError(
                                    String.format("cannot find any backup storage to upload the template created from the snapshot[uuid:%s, name:%s]",
                                            currentLeaf.getUuid(), currentLeaf.getInventory().getName())
                            ));
                        }

                        for (String bsUuid : bsUuids) {
                            BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(bsUuid);
                            if (updater.reserveCapacity(actualSize, false)) {
                                targetBackupStorageUuids.add(bsUuid);
                            } else {
                                //TODO
                                logger.warn(String.format("unable to reserve the capacity[%s bytes] on the backup storage[uuid:%s], it's short of capacity",
                                        actualSize, bsUuid));
                            }
                        }

                        if (targetBackupStorageUuids.isEmpty()) {
                            throw new OperationFailureException(errf.stringToOperationError(String.format("cannot reserve the capacity[%s bytes] on" +
                                    " all backup storage%s", actualSize, targetBackupStorageUuids)));
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (String bsUuid : targetBackupStorageUuids) {
                            BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(bsUuid);
                            updater.increaseAvailableCapacity(actualSize);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-to-backup-storage";
                    List<ErrorCode> errors = new ArrayList<ErrorCode>();
                    boolean success = false;
                    String mediaType;

                    {
                        SimpleQuery<ImageVO> q = dbf.createQuery(ImageVO.class);
                        q.select(ImageVO_.mediaType);
                        q.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
                        mediaType = q.findValue();
                    }


                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final AsyncLatch latch = new AsyncLatch(targetBackupStorageUuids.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!success) {
                                    trigger.fail(errf.stringToOperationError(
                                            "failed to upload the image to all backup storage", errors
                                    ));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (String bsUuid : targetBackupStorageUuids) {
                            upload(bsUuid, new Completion() {
                                @Override
                                public void success() {
                                    success = true;
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errors.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }

                    private void upload(final String bsUuid, final Completion completion) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setImageMediaType(mediaType);
                        bmsg.setImageUuid(msg.getImageUuid());
                        bmsg.setBackupStorageUuid(bsUuid);
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            throw new OperationFailureException(errf.stringToOperationError(
                                    String.format("unable to get installation path on the backup storage[uuid:%s]", bsUuid), reply.getError()
                            ));
                        }

                        final String bsInstallPath = ((BackupStorageAskInstallPathReply)br).getInstallPath();
                        PrimaryStorageUploadBitsToBackupStorageMsg umsg = new PrimaryStorageUploadBitsToBackupStorageMsg();
                        umsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        umsg.setPrimaryStorageInstallPath(temporaryInstallPath);
                        umsg.setBackupStorageUuid(bsUuid);
                        umsg.setBackupStorageInstallPath(bsInstallPath);
                        bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(umsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateTemplateFromVolumeSnapshotResult result = new CreateTemplateFromVolumeSnapshotResult();
                                    result.setBackupStorageUuid(bsUuid);
                                    result.setInstallPath(bsInstallPath);
                                    results.add(result);
                                    completion.success();
                                } else {
                                    completion.fail(errf.stringToOperationError(String.format("unable to upload the image to the backup storage[uuid:%s]", bsUuid), reply.getError()));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-the-temporary-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DeleteBitsOnPrimaryStorageMsg dmsg = new DeleteBitsOnPrimaryStorageMsg();
                        dmsg.setInstallPath(temporaryInstallPath);
                        dmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(dmsg);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setResults(info.results);
                        reply.setSize(info.bitsSize);
                        reply.setActualSize(info.bitsActualSize);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteVolumeSnapshotMsg) {
            handle((APIDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromSnapshotMsg) {
            handle((APIRevertVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof APIBackupVolumeSnapshotMsg) {
            handle((APIBackupVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotFromBackupStorageMsg) {
            handle((APIDeleteVolumeSnapshotFromBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotMsg) {
            handle((APIUpdateVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotMsg msg) {
        VolumeSnapshotVO self = dbf.findByUuid(msg.getUuid(), VolumeSnapshotVO.class);

        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeSnapshotEvent evt = new APIUpdateVolumeSnapshotEvent(msg.getId());
        evt.setInventory(VolumeSnapshotInventory.valueOf(self));
        bus.publish(evt);
    }

    private boolean cleanup() {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.primaryStorageUuid);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, currentRoot.getUuid());
        String uuid = q.findValue();

        if (uuid != null) {
            return false;
        }

        SimpleQuery<VolumeSnapshotBackupStorageRefVO> bq = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
        bq.add(VolumeSnapshotBackupStorageRefVO_.volumeSnapshotUuid, Op.EQ, currentRoot.getUuid());
        boolean onBs = bq.isExists();

        if (onBs) {
            return false;
        }

        // the snapshot is on neither primary storage nor backup storage. delete it and descendants
        List<String> uuids = CollectionUtils.transformToList(currentLeaf.getDescendants(), new Function<String, VolumeSnapshotInventory>() {
            @Override
            public String call(VolumeSnapshotInventory arg) {
                return arg.getUuid();
            }
        });

        dbf.removeByPrimaryKeys(uuids, VolumeSnapshotVO.class);

        SimpleQuery<VolumeSnapshotVO> tq = dbf.createQuery(VolumeSnapshotVO.class);
        tq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, currentRoot.getTreeUuid());
        if (!tq.isExists()) {
            logger.debug(String.format("volume snapshot tree[uuid:%s] has no leaf, delete it", currentRoot.getTreeUuid()));
            dbf.removeByPrimaryKey(currentRoot.getTreeUuid(), VolumeSnapshotTreeVO.class);
        }

        return true;
    }

    private List<VolumeSnapshotBackupStorageDeletionMsg> makeVolumeSnapshotBackupStorageDeletionMsg(List<String> bsUuids) {
        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = new ArrayList<VolumeSnapshotBackupStorageDeletionMsg>();

        List<String> allMyBsUuids = CollectionUtils.transformToList(currentRoot.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefVO>() {
            @Override
            public String call(VolumeSnapshotBackupStorageRefVO arg) {
                return arg.getBackupStorageUuid();
            }
        });

        if (allMyBsUuids.isEmpty()) {
            return msgs;
        }

        if (bsUuids == null || bsUuids.containsAll(allMyBsUuids)) {
            // delete me and all my descendants
            for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
                List<String> buuids = CollectionUtils.transformToList(inv.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefInventory>() {
                    @Override
                    public String call(VolumeSnapshotBackupStorageRefInventory arg) {
                        return arg.getBackupStorageUuid();
                    }
                });
                VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
                msg.setBackupStorageUuids(buuids);
                msg.setSnapshotUuid(inv.getUuid());
                bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                msgs.add(msg);
            }
        } else {
            // delete me only
            VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
            msg.setSnapshotUuid(currentRoot.getUuid());
            msg.setBackupStorageUuids(bsUuids);
            bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
            msgs.add(msg);
        }

        return msgs;
    }

    private void handle(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deleteOnBackupStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s-on-backup-storage", currentRoot.getUuid());
            }
        });
    }

    private void deleteOnBackupStorage(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg, final NoErrorCompletion completion) {
        final APIDeleteVolumeSnapshotFromBackupStorageEvent evt = new APIDeleteVolumeSnapshotFromBackupStorageEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-from-backup-stroage", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-snapshot-from-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = makeVolumeSnapshotBackupStorageDeletionMsg(msg.getBackupStorageUuids());
                        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                //delete would always succeed
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        if (!cleanup()) {
                            currentRoot = dbf.reload(currentRoot);
                            evt.setInventory(getSelfInventory());
                        }

                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final APIBackupVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask() {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                backup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("backup-volume-snapshot-%s", msg.getUuid());
            }
        });
    }

    private void backup(final APIBackupVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final APIBackupVolumeSnapshotEvent evt = new APIBackupVolumeSnapshotEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        class Info {
            VolumeSnapshotInventory snapshot;
            String backupStorageUuid;
            BackupStorageInventory destBackupStorage;
            boolean backupSuccess;
        }

        final String requiredBsUuid = msg.getBackupStorageUuid();
        final List<Info> needBackup = new ArrayList<Info>();
        currentLeaf.walkUp(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                Info info = new Info();
                info.snapshot = arg;

                if (arg.getUuid().equals(currentRoot.getUuid()) && requiredBsUuid != null) {
                    info.backupStorageUuid = requiredBsUuid;
                    needBackup.add(info);
                    return false;
                }

                if (arg.getBackupStorageRefs().isEmpty()) {
                    needBackup.add(info);
                }

                return false;
            }
        });

        if (needBackup.isEmpty()) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("backup-volume-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-backup-storage";

                    private void allocateBackupStorage(final Iterator<Info> it, final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            trigger.next();
                            return;
                        }

                        final Info info = it.next();
                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setBackupStorageUuid(info.backupStorageUuid);
                        amsg.setSize(info.snapshot.getSize());
                        bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateBackupStorageReply re = reply.castReply();
                                    info.destBackupStorage = re.getInventory();
                                    allocateBackupStorage(it, trigger);
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        allocateBackupStorage(needBackup.iterator(), trigger);
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (Info info : needBackup) {
                            if (info.destBackupStorage != null) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(info.destBackupStorage.getUuid());
                                rmsg.setSize(info.snapshot.getSize());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, info.destBackupStorage.getUuid());
                                bus.send(rmsg);
                            }
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "backing-up";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<BackupVolumeSnapshotMsg> bmsgs = CollectionUtils.transformToList(needBackup, new Function<BackupVolumeSnapshotMsg, Info>() {
                            @Override
                            public BackupVolumeSnapshotMsg call(Info arg) {
                                BackupVolumeSnapshotMsg bmsg = new BackupVolumeSnapshotMsg();
                                bmsg.setBackupStorage(arg.destBackupStorage);
                                bmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(bmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                return bmsg;
                            }
                        });

                        bus.send(bmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                ErrorCode err = null;
                                for (MessageReply r : replies) {
                                    if (r.isSuccess()) {
                                        Info info = needBackup.get(replies.indexOf(r));
                                        info.backupSuccess = true;
                                        logger.debug(String.format("successfully backed up volume snapshot[uuid:%s] on backup storage[uuid:%s]",
                                                info.snapshot.getUuid(), info.destBackupStorage.getUuid()));
                                    } else {
                                        err = r.getError();
                                    }
                                }

                                if (err != null) {
                                    trigger.fail(err);
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = CollectionUtils.transformToList(needBackup, new Function<VolumeSnapshotBackupStorageDeletionMsg, Info>() {
                            @Override
                            public VolumeSnapshotBackupStorageDeletionMsg call(Info arg) {
                                if (arg.backupSuccess) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = new VolumeSnapshotBackupStorageDeletionMsg();
                                    dmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                    dmsg.setBackupStorageUuids(Arrays.asList(arg.destBackupStorage.getUuid()));
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                    return dmsg;
                                }
                                return null;
                            }
                        });

                        if (dmsgs.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        refreshVO();
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final APIRevertVolumeFromSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                revert(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid());
            }
        });
    }

    private void revert(final APIRevertVolumeFromSnapshotMsg msg, final NoErrorCompletion completion) {
        final APIRevertVolumeFromSnapshotEvent evt = new APIRevertVolumeFromSnapshotEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            String newVolumeInstallPath;
            VolumeVO volume = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
            VolumeInventory volumeInventory = VolumeInventory.valueOf(volume);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "revert-volume-from-volume-snapshot-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RevertVolumeFromSnapshotOnPrimaryStorageMsg rmsg = new RevertVolumeFromSnapshotOnPrimaryStorageMsg();
                        rmsg.setSnapshot(getSelfInventory());
                        rmsg.setVolume(volumeInventory);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volumeInventory.getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    RevertVolumeFromSnapshotOnPrimaryStorageReply re = (RevertVolumeFromSnapshotOnPrimaryStorageReply) reply;
                                    newVolumeInstallPath = re.getNewVolumeInstallPath();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Transactional
                    private void updateLatest() {
                        String sql = "update VolumeSnapshotVO s set s.latest = false where s.latest = true and s.treeUuid = :treeUuid";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();

                        currentRoot.setLatest(true);
                        dbf.getEntityManager().merge(currentRoot);

                        sql = "update VolumeSnapshotTreeVO tree set tree.current = false where tree.current = true and tree.volumeUuid = :volUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("volUuid", currentRoot.getVolumeUuid());
                        q.executeUpdate();

                        sql = "update VolumeSnapshotTreeVO tree set tree.current = true where tree.uuid = :treeUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();
                    }

                    @Override
                    public void handle(Map data) {
                        volume.setInstallPath(newVolumeInstallPath);
                        dbf.update(volume);
                        updateLatest();
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to restore volume[uuid:%s] to snapshot[uuid:%s, name:%s], %s",
                                volumeInventory.getUuid(), currentRoot.getUuid(), currentRoot.getName(), errCode));
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();

    }

    private void handle(final APIDeleteVolumeSnapshotMsg msg) {
        final APIDeleteVolumeSnapshotEvent evt = new APIDeleteVolumeSnapshotEvent(msg.getId());
        final String issuer = VolumeSnapshotVO.class.getSimpleName();
        final List<VolumeSnapshotInventory> ctx = Arrays.asList(getSelfInventory());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-snapshot-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }
}
