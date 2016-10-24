package org.zstack.hotfix;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/10/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HotFix1169 implements HotFix {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private APIHotFix1169KvmSnapshotChainMsg msg;
    private APIHotFix1169KvmSnapshotChainEvent evt;

    private PrimaryStorageVO primaryStorageVO;
    private List<String> attachedKvmClusterUuids;
    private List<VolumeVO> volumesReady;

    /*
#!/bin/bash

all=`find $1 -type f -exec file {} \; | awk -F: '$2~/QEMU QCOW/{print $1}'`
for f in $all
do
    bk=`qemu-img info $f | grep -w 'backing file:' | awk '{print $3}'`
    if [ x"$bk" == "x" ]; then
        bk="NONE"
    fi

    size=`ls -l $f | awk '{print $5}'`
    if [ x"$size" == "x" ]; then
        size="NONE"
    fi

    date=`stat -c %Y $f`
    if [ x"$date" == "x" ]; then
        date="NONE"
    fi

    echo $f $bk $size $date
done
     */
    private String READ_QCOW2_SCRIPT = "#!/bin/bash\n" +
            "\n" +
            "all=`find $1 -type f -exec file {} \\; | awk -F: '$2~/QEMU QCOW/{print $1}'`\n" +
            "for f in $all\n" +
            "do\n" +
            "    bk=`qemu-img info $f | grep -w 'backing file:' | awk '{print $3}'`\n" +
            "    if [ x\"$bk\" == \"x\" ]; then\n" +
            "        bk=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    size=`ls -l $f | awk '{print $5}'`\n" +
            "    if [ x\"$size\" == \"x\" ]; then\n" +
            "        size=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    date=`stat -c %Y $f`\n" +
            "    if [ x\"$date\" == \"x\" ]; then\n" +
            "        date=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    echo $f $bk $size $date\n" +
            "done";

    class LocalStorageHotFix {
        void fix() {

        }
    }

    class NfsHostFix {
        void fix() {
            List<String> connectedKvmHosts = findConnectedKvmHosts();
            KvmRunShellReply r = null;
            List<ErrorCode> errors = new ArrayList<ErrorCode>();
            for (String huuid : connectedKvmHosts) {
                KvmRunShellMsg msg = new KvmRunShellMsg();
                msg.setHostUuid(huuid);
                msg.setScript(READ_QCOW2_SCRIPT);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
                MessageReply reply = bus.call(msg);
                if (reply.isSuccess()) {
                    r = reply.castReply();
                    break;
                } else {
                    errors.add(reply.getError());
                }
            }

            if (r == null) {
                throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        "no kvm host succeed to get QCOW2 file information", errors));
            }
        }

        private List<String> findConnectedKvmHosts() {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid);
            q.add(HostVO_.clusterUuid, Op.IN, attachedKvmClusterUuids);
            q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
            return q.listValue();
        }
    }

    @Transactional(readOnly = true)
    private void findAttachedKvmClusters() {
        String sql = "select ref.clusterUuid from PrimaryStorageClusterRefVO ref, ClusterVO c where" +
                " c.uuid = ref.clusterUuid and ref.primaryStorageUuid = :psUuid and c.hypervisorType = :hvType";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", primaryStorageVO.getUuid());
        q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
        attachedKvmClusterUuids = q.getResultList();
        if (attachedKvmClusterUuids.isEmpty()) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the primary storage[uuid:%s, name:%s] is not attached to any KVM clusters",
                            primaryStorageVO.getUuid(), primaryStorageVO.getName())
            ));
        }
    }

    public HotFix1169(APIHotFix1169KvmSnapshotChainMsg msg) {
        this.msg = msg;
        evt = new APIHotFix1169KvmSnapshotChainEvent(msg.getId());
    }

    public void fix() {
        primaryStorageVO = dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (!primaryStorageVO.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)
                && !primaryStorageVO.getType().equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the hotfix1169 is only for primary storage with type[%s, %s], but the" +
                                    " primary storage[uuid:%s] is of type[%s]", LocalStorageConstants.LOCAL_STORAGE_TYPE,
                            NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE, primaryStorageVO.getUuid(),
                            primaryStorageVO.getType())
            ));
        }

        findAttachedKvmClusters();

        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
        q.add(VolumeVO_.primaryStorageUuid, Op.EQ, primaryStorageVO.getUuid());
        volumesReady = q.listValue();
        if (volumesReady.isEmpty()) {
            // no volumes
            bus.publish(evt);
            return;
        }

        if (primaryStorageVO.getType().equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            new NfsHostFix().fix();
        } else {
            new LocalStorageHotFix().fix();
        }
    }
}
