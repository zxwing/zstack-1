package org.zstack.hotfix;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by xing5 on 2016/10/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HotFix1169 implements HotFix {
    private static CLogger logger = Utils.getLogger(HotFix1169.class);

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

    class NodeException extends Exception {
        ErrorCode error;

        public NodeException(ErrorCode err) {
            error = err;
        }
    }

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

    class Node {
        Node parent;
        Map<String, Node> children = new HashMap<String, Node>();
        String path;
        Long size;
        long lastModificationTime;

        Node findAncient() {
            if (parent == null) {
                return this;
            }

            Node p = parent;
            while (p.parent != null) {
                p = p.parent;
            }

            return p;
        }

        boolean isLeaf() {
            return children.isEmpty();
        }
    }

    class Fixer {
        Map<String, Node> nodesOnStorage;
        Map<String, Node> nodesInDb;
        // start point.
        // for root volume, it's image cache path
        // for data volume, it's the first snapshot
        String start;
        // end point
        // it's current volume path
        String end;
        VolumeVO volume;
        String treeUuid;
        HotFix1169Result result = new HotFix1169Result();

        boolean hasMissingSnapshots;

        @Transactional
        HotFix1169Result fix() {
            result.volumeName = volume.getName();
            result.volumeUuid = volume.getUuid();

            boolean startMustInDb = new Callable<Boolean>() {
                public Boolean call() {
                    if (volume.getType() == VolumeType.Root) {
                        return true;
                    } else {
                        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, volume.getUuid());
                        return q.count() > 0;
                    }
                }
            }.call();

            if (startMustInDb) {
                // the volume is a root volume or
                // it's a data volume with snapshots
                Node startNodeInDb = nodesInDb.get(start);
                DebugUtils.Assert(startNodeInDb != null, "startNodeInDb is null");
                Node startNodeOnStorage = nodesOnStorage.get(start);
                DebugUtils.Assert(startNodeOnStorage != null, "startNodeOnStorage is null");

                Stack<String> path = new Stack<String>();
                try {
                    compareNodes(startNodeInDb, startNodeOnStorage, path);

                    logger.debug(String.format("[HOTFIX 1169] Snapshot tree check passed for the volume[name:%s, uuid:%s]",
                            volume.getName(), volume.getUuid()));

                    walkAndFix(startNodeOnStorage, startNodeInDb);
                } catch (NodeException e) {
                    result.setError(e.error.getDetails());
                }

            } else {
                // data volume with no snapshots
                Node startNodeInDb = nodesInDb.get(start);
                if (startNodeInDb != null) {
                    if (!startNodeInDb.path.equals(volume.getInstallPath())) {
                        result.setError(String.format("The data volume[uuid:%s, name:%s] has a unknown parent[%s] in" +
                                " the database", volume.getUuid(), volume.getName(), startNodeInDb.path));
                    }
                    // else the node is ok, no need to fix
                } else {
                    DebugUtils.Assert(treeUuid == null, "why treeUuid has value");
                    treeUuid = createNewTree();

                    Node startNodeOnStorage = nodesOnStorage.get(start);
                    DebugUtils.Assert(startNodeOnStorage != null, "startNodeOnStorage is null");

                    insertMissingSnapshotInDatabase(startNodeOnStorage);
                }
            }

            if (hasMissingSnapshots) {
                recalculateVolumePathAndSize();
            }

            return result;
        }

        @Transactional
        private void recalculateVolumePathAndSize() {
            Node nodeOnStorage = nodesOnStorage.get(volume.getInstallPath());

            final List<Node> leaves = new ArrayList<Node>();
            class FindLeaf {
                void find(Node n) {
                    if (n.isLeaf()) {
                        leaves.add(n);
                    } else {
                        for (Node nn : n.children.values()) {
                            find(nn);
                        }
                    }
                }
            }
            new FindLeaf().find(nodeOnStorage);

            // find the last modified qcow2 if there are more than one
            Node l = leaves.get(0);
            for (Node ll : leaves) {
                if (ll.lastModificationTime > l.lastModificationTime) {
                    l = ll;
                }
            }

            result.addDetail(String.format("fixed installPath from %s to %s, size from %s to %s for the volume" +
                    "[uuid:%s, name:%s]", volume.getInstallPath(), l.path, volume.getSize(), l.size, volume.getUuid(),
                    volume.getName()));

            volume.setActualSize(l.size);
            volume.setInstallPath(l.path);
            dbf.getEntityManager().merge(volume);
        }

        @Transactional
        private void walkAndFix(Node snode, Node dbnode) {
            walkAndFixMissingSnapshot(snode, dbnode);
        }


        private String createNewTree() {
            // a missing new tree
            VolumeSnapshotTreeVO t = new VolumeSnapshotTreeVO();
            t.setCurrent(true);
            t.setVolumeUuid(volume.getUuid());
            t.setUuid(Platform.getUuid());
            dbf.persist(t);

            logger.debug(String.format("[HOTFIX 1169] created a new snapshot tree[uuid:%s] for the volume[uuid:%s, name:%s]",
                    t.getUuid(), volume.getUuid(), volume.getName()));
            return t.getUuid();

        }

        private void walkAndFixMissingSnapshot(Node snode, Node dbnode) {
            for (Node scnode : snode.children.values()) {
                Node dbcnode = dbnode.children.get(scnode.path);
                if (dbcnode != null) {
                    walkAndFixMissingSnapshot(scnode, dbcnode);
                } else {
                    logger.debug(String.format("[HOTFIX 1169] found %s missing in the DB snapshot tree of the volume[uuid:%s, name:%s]",
                            scnode.parent, volume.getUuid(), volume.getName()));

                    if (treeUuid == null) {
                        treeUuid = createNewTree();
                    }

                    insertMissingSnapshotInDatabase(scnode);
                }
            }
        }

        @Transactional
        private void insertMissingSnapshotInDatabase(final Node scnode) {
            VolumeSnapshotVO spvo = new VolumeSnapshotVO();
            spvo.setUuid(Platform.getUuid());
            spvo.setName(String.format("sp-for-volume-%s", volume.getName()));
            spvo.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
            spvo.setFormat(volume.getFormat());
            spvo.setDistance(new Callable<Integer>() {
                public Integer call() {
                    int d = 0;
                    Node p = scnode;
                    while (p.parent != null) {
                        p = p.parent;
                        d ++;
                    }
                    return d;
                }
            }.call());
            spvo.setFullSnapshot(false);
            spvo.setLatest(scnode.isLeaf());
            spvo.setSize(scnode.size);
            spvo.setStatus(VolumeSnapshotStatus.Ready);
            spvo.setState(VolumeSnapshotState.Enabled);
            spvo.setPrimaryStorageInstallPath(scnode.path);
            spvo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
            spvo.setVolumeUuid(volume.getUuid());
            spvo.setVolumeType(volume.getType().toString());
            spvo.setTreeUuid(treeUuid);

            if (scnode.parent != null) {
                SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                q.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.EQ, scnode.parent.path);
                VolumeSnapshotVO parent = q.find();
                DebugUtils.Assert(parent != null, String.format("the orphan snapshot[%s]'s parent[%s] has no record in our database",
                        scnode.path, scnode.parent.path));
                spvo.setParentUuid(parent.getUuid());
            }

            dbf.getEntityManager().persist(spvo);

            hasMissingSnapshots = true;
            result.addDetail(String.format("fixed a missing snapshot[uuid:%s, path:%s]", spvo.getUuid(), spvo.getPrimaryStorageInstallPath()));
        }

        private void compareNodes(Node left, Node right, Stack<String> path) throws NodeException {
            if (!left.path.equals(right.path)) {
                List<String> pathIndb = new ArrayList<String>();
                pathIndb.addAll(path);
                pathIndb.add(left.path);

                List<String> pathOnStorage = new ArrayList<String>();
                pathOnStorage.addAll(path);
                pathOnStorage.add(right.path);

                String err = String.format(
                        "diverged snapshot tree:\n" +
                                "volume[name:%s, uuid:%s]'s snapshot tree is diverged between database and storage\n" +
                                "path in database:\n%s\n" +
                                "path on storage:\n%s\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                        StringUtils.join(pathOnStorage, " --> "));
                logger.warn(err);
                throw new NodeException(errf.stringToOperationError(err));
            }

            if (left.isLeaf()) {
                return;
            }

            path.push(left.path);

            for (Node child : left.children.values()) {
                Node rightChild = right.children.get(child.path);
                if (rightChild == null) {
                    List<String> pathIndb = new ArrayList<String>();
                    pathIndb.addAll(path);
                    pathIndb.add(child.path);

                    List<String> pathOnStorage = new ArrayList<String>();
                    pathOnStorage.addAll(path);
                    String err =  String.format(
                            "diverged snapshot tree:\n" +
                                    "volume[name:%s, uuid:%s]'s snapshot tree on storage is shorter than the tree in the database\n" +
                                    "path in database:\n%s\n" +
                                    "path on storage:\n%s\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                            StringUtils.join(pathOnStorage, " --> "));
                    logger.warn(err);
                    throw new NodeException(errf.stringToOperationError(err));
                }

                compareNodes(child, rightChild, path);
            }

            path.pop();
        }
    }

    class LocalStorageHotFix {
        void fix() {

        }
    }

    class NfsHostFix {
        void fix() {
            List<HotFix1169Result> results = new ArrayList<HotFix1169Result>();

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

            if (r.getReturnCode() != 0) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("failed to collected qcow2 info on the primary storage. ret code:%s," +
                                "stdout: %s, stderr: %s", r.getReturnCode(), r.getStdout(), r.getStderr())
                ));
            }

            Map<String, Node> nodes = buildNodes(r.getStdout());

            for (VolumeVO vol : volumesReady) {
                Fixer fixer = new Fixer();
                fixer.nodesOnStorage = nodes;
                fixer.nodesInDb = new HashMap<String, Node>();
                fixer.treeUuid = buildNodesInDb(vol, fixer.nodesInDb);
                fixer.volume = vol;

                if (vol.getType() == VolumeType.Root) {
                    SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
                    iq.add(ImageCacheVO_.imageUuid, Op.EQ, vol.getRootImageUuid());
                    ImageCacheVO cache = iq.find();
                    DebugUtils.Assert(cache != null, String.format("cannot find image cache for the volume[uuid:%s, name:%s]",
                            vol.getUuid(), vol.getName()));

                    fixer.start = cache.getInstallUrl();
                    fixer.end = vol.getInstallPath();
                } else {
                    Node node = nodes.get(vol.getInstallPath());
                    Node ancient = node.findAncient();
                    fixer.start = ancient.path;
                    fixer.end = vol.getInstallPath();
                }

                HotFix1169Result res = fixer.fix();
                if (res.error != null || !res.details.isEmpty()) {
                    // a hotfix applied
                    results.add(res);
                }
            }

            evt.setResults(results);
        }

        private Map<String, Node> buildNodesFromTreeUuid(final String startPath, String treeUuid) {
            final Map<String, Node> nodes = new HashMap<String, Node>();

            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, treeUuid);
            List<VolumeSnapshotVO> sps = q.list();
            final VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(sps);

            if (startPath != null) {
                // the tree is linked to a image cache
                // image cache(parent) --> root snapshot(child)
                Node root = new Node();
                root.path = tree.getRoot().getInventory().getPrimaryStorageInstallPath();

                Node p = new Node();
                p.path = startPath;
                p.children.put(root.path, root);
                root.parent = p;

                nodes.put(root.path, root);
                nodes.put(p.path, p);
            }

            tree.getRoot().walk(new Function<Void, SnapshotLeaf>() {
                public Void call(SnapshotLeaf arg) {
                    if (startPath != null && arg.getUuid().equals(tree.getRoot().getUuid())) {
                        // the tree is linked to a image cache, skip the root as
                        // we have manually add the relationship
                        // image cache(parent) --> root snapshot(child)
                        return null;
                    }

                    Node n = nodes.get(arg.getInventory().getPrimaryStorageInstallPath());
                    if (n == null) {
                        n = new Node();
                        n.path = arg.getInventory().getPrimaryStorageInstallPath();
                        nodes.put(n.path, n);
                    }

                    if (arg.getParent() != null) {
                        Node p = nodes.get(arg.getParent().getInventory().getPrimaryStorageInstallPath());
                        if (p == null) {
                            p = new Node();
                            p.path = arg.getParent().getInventory().getPrimaryStorageInstallPath();
                            nodes.put(p.path, p);
                        }

                        p.children.put(n.path, n);
                        n.parent = p;
                    }

                    return null;
                }
            });

            return nodes;
        }

        private String buildNodesInDb(VolumeVO vol, Map<String, Node> nodes) {
            String treeUuid = null;

            if (vol.getType() == VolumeType.Root) {
                SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
                iq.add(ImageCacheVO_.imageUuid, Op.EQ, vol.getRootImageUuid());
                ImageCacheVO cache = iq.find();
                DebugUtils.Assert(cache != null, String.format("cannot find image cache for the volume[uuid:%s, name:%s]",
                        vol.getUuid(), vol.getName()));

                SimpleQuery<VolumeSnapshotTreeVO> tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                long count = tq.count();

                if (count == 0) {
                    // no snapshot
                    Node n = new Node();
                    n.path = vol.getInstallPath();

                    Node p = new Node();
                    p.path = cache.getInstallUrl();
                    p.children.put(n.path, n);
                    n.parent = p;

                    nodes.put(n.path, n);
                    nodes.put(p.path, p);
                } else if (count == 1) {
                    // one snapshot tree
                    tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                    tq.select(VolumeSnapshotTreeVO_.uuid);
                    tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                    treeUuid = tq.findValue();

                    nodes.putAll(buildNodesFromTreeUuid(cache.getInstallUrl(), treeUuid));
                } else {
                    // multiple snapshot trees
                    tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                    tq.select(VolumeSnapshotTreeVO_.uuid);
                    tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                    tq.add(VolumeSnapshotTreeVO_.current, Op.EQ, true);
                    treeUuid = tq.findValue();

                    nodes.putAll(buildNodesFromTreeUuid(null, treeUuid));
                }
            } else {
                SimpleQuery<VolumeSnapshotTreeVO> tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                long count = tq.count();

                if (count == 0) {
                    nodes = new HashMap<String, Node>();
                    // no snapshot
                    Node n = new Node();
                    n.path = vol.getInstallPath();
                    nodes.put(n.path, n);
                } else {
                    // has snapshot trees
                    tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                    tq.select(VolumeSnapshotTreeVO_.uuid);
                    tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                    tq.add(VolumeSnapshotTreeVO_.current, Op.EQ, true);
                    treeUuid = tq.findValue();
                    nodes.putAll(buildNodesFromTreeUuid(null, treeUuid));
                }
            }

            return treeUuid;
        }

        private List<String> findConnectedKvmHosts() {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid);
            q.add(HostVO_.clusterUuid, Op.IN, attachedKvmClusterUuids);
            q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
            return q.listValue();
        }
    }

    private Map<String, Node> buildNodes(String qcow2InfoRawOutput) {
        Map<String, Node> nodes = new HashMap<String, Node>();

        for (String s : qcow2InfoRawOutput.split("\n")) {
            s = s.trim().replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "");
            if (s.isEmpty()) {
                continue;
            }

            String[] parts = s.split(" ");
            if (parts.length != 4) {
                throw new CloudRuntimeException(String.format("invalid qcow2 raw info: %s", s));
            }

            String path = parts[0];
            String backingFile = parts[1];
            long size = Long.valueOf(parts[2]);
            long lastModified = Long.valueOf(parts[3]);

            Node node = nodes.get(path);
            if (node == null) {
                node = new Node();
                node.path = path;
                nodes.put(node.path, node);
            }

            node.size = size;
            node.lastModificationTime = lastModified;

            Node parent = nodes.get(backingFile);
            if (parent == null) {
                parent = new Node();
                parent.path = backingFile;
                nodes.put(backingFile, parent);
            }

            parent.children.put(node.path, node);
            node.parent = parent;
        }

        return nodes;
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
        volumesReady = q.list();
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

        bus.publish(evt);
    }
}
