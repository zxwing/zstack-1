package org.zstack.storage.boss;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;

import javax.persistence.LockModeType;

/**
 * Created by XXPS-PC1 on 2016/11/15.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BossCapacityUpdater {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    public void update(String clusterName, long total, long avail) {
        update(clusterName, total, avail, true);
    }

    @Transactional
    public void update(String clusterName, long total, long avail, boolean updatedAnyway) {
        BossCapacityVO vo = dbf.getEntityManager().find(BossCapacityVO.class, clusterName, LockModeType.PESSIMISTIC_WRITE);
        boolean updated = false;

        if (vo == null) {
            GLock lock = new GLock(String.format("Boss-%s", clusterName), 120);
            lock.lock();
            try {
                vo = dbf.getEntityManager().find(BossCapacityVO.class, clusterName, LockModeType.PESSIMISTIC_WRITE);
                if (vo == null) {
                    vo = new BossCapacityVO();
                    vo.setClusterName(clusterName);
                    vo.setTotalCapacity(total);
                    vo.setAvailableCapacity(avail);
                    dbf.getEntityManager().persist(vo);
                    updated = true;
                } else {
                    if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                        vo.setTotalCapacity(total);
                        vo.setAvailableCapacity(avail);
                        dbf.getEntityManager().merge(vo);
                        updated = true;
                    }
                }
            } finally {
                lock.unlock();
            }
        } else  {
            if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                vo.setTotalCapacity(total);
                vo.setAvailableCapacity(avail);
                dbf.getEntityManager().merge(vo);
                updated = true;
            }
        }

        if (updatedAnyway || updated) {
            for (BossCapacityUpdateExtensionPoint ext : pluginRgty.getExtensionList(BossCapacityUpdateExtensionPoint.class)) {
                ext.update(clusterName, total, avail);
            }
        }
    }

}
