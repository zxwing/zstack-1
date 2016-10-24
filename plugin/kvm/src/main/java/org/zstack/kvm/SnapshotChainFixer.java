package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainEvent.FixResult;
import org.zstack.kvm.APIKvmFixVolumeSnapshotChainMsgHandler.Qcow2FileInfo;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/10/24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SnapshotChainFixer {
    private static CLogger logger = Utils.getLogger(SnapshotChainFixer.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private VolumeVO volume;
    private List<Qcow2FileInfo> qcow2FileInfo;

    private class Node {
        String path;
        Node parent;
        long size;
        long lastModification;
        LinkedHashMap<String, Node> children = new LinkedHashMap<>();

        public Node(String path) {
            this.path = path;
        }

        public Node addNode(Node n) {
            children.put(n.path, n);
            return n;
        }
    }

    private Map<String, Node> nodes = new HashMap<>();
    private FixResult result;

    public SnapshotChainFixer(VolumeVO volume, List<Qcow2FileInfo> qcow2FileInfo) {
        this.volume = volume;
        this.qcow2FileInfo = qcow2FileInfo;
    }

    private void walkAndFixMissingSnapshot(VolumeVO vol, Node node) {
        if (node.children.isEmpty()) {
            return;
        }

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.EQ, node.path);
        VolumeSnapshotVO sp = q.find();
        if (sp == null) {
            VolumeSnapshotVO spvo = new VolumeSnapshotVO();
            spvo.setUuid(Platform.getUuid());
            spvo.setName(String.format("sp-for-volume-%s", vol.getName()));
            spvo.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
            spvo.setFormat(vol.getFormat());
            // need to re-calculate
            spvo.setDistance(0);
            spvo.setFullSnapshot(false);
            // need to re-calculate
            spvo.setLatest(node.children.isEmpty());
            spvo.setSize(node.size);
            spvo.setStatus(VolumeSnapshotStatus.Ready);
            spvo.setState(VolumeSnapshotState.Enabled);
            spvo.setPrimaryStorageInstallPath(node.path);
            spvo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
            spvo.setVolumeUuid(vol.getUuid());
            spvo.setVolumeType(vol.getType().toString());

            if (node.parent != null) {
                q = dbf.createQuery(VolumeSnapshotVO.class);
                q.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.EQ, node.parent.path);
                VolumeSnapshotVO parent = q.find();
                DebugUtils.Assert(parent != null, String.format("the orphan snapshot[%s]'s parent[%s] has no record in our database",
                        node.path, node.parent.path));
                spvo.setParentUuid(parent.getUuid());
                spvo.setTreeUuid(parent.getTreeUuid());
            } else {
                // a missing new tree
                VolumeSnapshotTreeVO t = new VolumeSnapshotTreeVO();
                t.setCurrent(true);
                t.setVolumeUuid(vol.getUuid());
                t.setUuid(Platform.getUuid());
                dbf.persist(t);
                logger.debug(String.format("created a new snapshot tree[uuid:%s] for the volume[uuid:%s, name:%s]",
                        t.getUuid(), vol.getUuid(), vol.getName()));

                spvo.setTreeUuid(t.getUuid());
                spvo.setParentUuid(null);
            }

            dbf.persist(spvo);

            result = new FixResult();
            result.setVolumeName(vol.getName());
            result.setVolumeUuid(vol.getUuid());
            result.setSuccess(true);
        }

        if (node.children != null && !node.children.isEmpty()) {
            for (Node n : node.children.values()) {
                walkAndFixMissingSnapshot(vol, n);
            }
        }
    }

    private void walkAndFixParentAndDistance(VolumeVO vol, Node node) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.EQ, node.path);
        VolumeSnapshotVO sp = q.find();
        DebugUtils.Assert(sp != null, String.format("why the snapshot[%s] has no record in the database", node.path));

        int distance = 0;
        Node p = node.parent;
        while (p != null) {
            distance ++;
            p = p.parent;
        }

        boolean update = false;
        if (sp.getDistance() != distance) {
            logger.debug(String.format("fix distance of the snapshot[uuid:%s, name:%s] %s -> %s", sp.getUuid(),
                    sp.getName(), sp.getDistance(), distance));
            sp.setDistance(distance);
            update = true;
        }

        if (sp.getParentUuid() != null) {
            DebugUtils.Assert(node.parent != null, String.format("the snapshot[uuid:%s, name:%s] has no parent on the primary storage",
                    sp.getUuid(), sp.getName()));
            VolumeSnapshotVO cp = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
            DebugUtils.Assert(cp != null, String.format("why the parent of the snapshot[uuid:%s, name:%s] not found",
                    sp.getUuid(), sp.getName()));

            q = dbf.createQuery(VolumeSnapshotVO.class);
            q.add(VolumeSnapshotVO_.primaryStorageInstallPath, Op.EQ, node.parent.path);
            VolumeSnapshotVO np = q.find();
            DebugUtils.Assert(np != null, String.format("why the snapshot[%s] has no record in the database", node.parent.path));

            if (!cp.getUuid().equals(np.getUuid())) {
                sp.setParentUuid(np.getUuid());
                logger.debug(String.format("fix parent of the snapshot[uuid:%s, name:%s] %s -> %s", sp.getUuid(),
                        sp.getName(), cp.getUuid(), np.getUuid()));
                update = true;
            }
        }

        if (update) {
            dbf.update(sp);
        }

        if (node.children != null && !node.children.isEmpty()) {
            for (Node n : node.children.values()) {
                walkAndFixParentAndDistance(vol, n);
            }
        }
    }

    private void findTheLatestOne(Node node, List<Node> latest) {
        if (node.children.isEmpty()) {
            latest.add(node);
            return;
        }

        for (Node c : node.children.values()) {
            findTheLatestOne(c, latest);
        }
    }

    private List<Node> treeToList(Node node, List<Node> lst) {
        if (node.children.size() > 1) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("the qcow2[%s] has more than one children %s", node.path,
                            node.children.values().stream().map(n -> n.parent).collect(Collectors.toList()))
            ));
        } else if (node.children.isEmpty()) {
            return lst;
        }

        Node n = node.children.values().iterator().next();
        lst.add(n);
        treeToList(n, lst);
        return lst;
    }

    public FixResult fix() {
        for (Qcow2FileInfo info : qcow2FileInfo) {
            Node me = nodes.get(info.getPath());
            if (me == null) {
                me = new Node(info.getPath());
                nodes.put(info.getPath(), me);
            }

            me.size = info.getSize();
            me.lastModification = info.getLastModificationTime();

            if (info.getBackingFile() != null) {
                Node p = nodes.get(info.getBackingFile());
                if (p == null) {
                    p = new Node(info.getBackingFile());
                    nodes.put(info.getBackingFile(), p);
                }

                p.addNode(me);
                me.parent = p;
            }
        }

        Node node = nodes.get(volume.getInstallPath());
        DebugUtils.Assert(node != null, String.format("cannot find the volume[uuid:%s, name:%s, path:%s] on" +
                " the primary storage", volume.getUuid(), volume.getName(), volume.getInstallPath()));

        if (node.children.isEmpty()) {
            // no snapshot
            return null;
        }

        List<Node> lst = treeToList(node, new ArrayList<>());



        walkAndFixMissingSnapshot(volume , node);

        walkAndFixParentAndDistance(volume, node);

        List<Node> latest = new ArrayList<>();
        findTheLatestOne(node, latest);
        DebugUtils.Assert(!latest.isEmpty(), "where is the latest node");
        Node l;
        if (latest.size() == 1) {
            l = latest.get(0);
        } else {
            l = latest.get(0);
            for (Node n : latest) {
                if (n.lastModification > l.lastModification) {
                    l = n;
                }
            }
        }

        volume.setInstallPath(l.path);
        volume.setActualSize(l.size);
        dbf.update(volume);

        return result;
    }
}
