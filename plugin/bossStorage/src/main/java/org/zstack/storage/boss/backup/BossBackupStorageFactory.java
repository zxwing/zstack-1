package org.zstack.storage.boss.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.boss.BossConstants;
import org.zstack.storage.boss.BossSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/11/9.
 */
public class BossBackupStorageFactory implements BackupStorageFactory, Component {

    private static final CLogger logger = Utils.getLogger(BossBackupStorageFactory.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ErrorFacade errf;

    public static final BackupStorageType type = new BackupStorageType(BossConstants.BOSS_BACKUP_STORAGE_TYPE);

    static {
        type.setOrder(900);
    }

    void init() {
        type.setPrimaryStorageFinder(new BackupStorageFindRelatedPrimaryStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findRelatedPrimaryStorage(String backupStorageUuid) {
                String sql = "select p.uuid from BossPrimaryStorageVO p, BossBackupStorageVO b where b.clusterName = p.clusterName" +
                        " and b.uuid = :buuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("buuid", backupStorageUuid);
                logger.info(String.format("the size of the list is %s",q.getResultList().size()));
                return q.getResultList();
            }
        });
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    @Transactional
    public BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddBossBackupStorageMsg bmsg = (APIAddBossBackupStorageMsg)msg;

        BossBackupStorageVO bvo = new BossBackupStorageVO(vo);
        bvo.setType(BossConstants.BOSS_BACKUP_STORAGE_TYPE);
        String poolName = bmsg.getPoolName() == null ? String.format("bak-t-%s", vo.getUuid()) : bmsg.getPoolName();
        String clusterName = bmsg.getClusterName() == null ? String.format("bak-c-%s",vo.getUuid()) : bmsg.getClusterName();
        bvo.setPoolName(poolName);
        bvo.setClusterName(clusterName);

        dbf.getEntityManager().persist(bvo);

        if (bmsg.getPoolName() != null) {
            BossSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.newSystemTagCreator(bvo.getUuid());
        }


        return BackupStorageInventory.valueOf(bvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        BossBackupStorageVO cvo = dbf.findByUuid(vo.getUuid(), BossBackupStorageVO.class);
        return new BossBackupStorageBase(cvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return BossBackupStorageInventory.valueOf(dbf.findByUuid(uuid, BossBackupStorageVO.class));
    }
}

