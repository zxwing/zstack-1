package org.zstack.storage.boss.primary;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.storage.boss.BossConstants;
import org.zstack.storage.boss.BossGlobalConfig;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by XXPS-PC1 on 2016/11/28.
 */
public class BossImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BossImageCacheCleaner.class);

    @Override
    protected String getPrimaryStorageType() {
        return  BossConstants.BOSS_PRIMARY_STORAGE_TYPE;
    }

    @Override
    protected GlobalConfig cleanupIntervalConfig() {
        return BossGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL;
    }

    @Transactional
    @Override
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs(String psUuid) {
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUuid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        String sql = "select ref.imageCacheId from ImageCacheVolumeRefVO ref where ref.imageCacheId in (:ids)";
        TypedQuery<Long> refq = dbf.getEntityManager().createQuery(sql, Long.class);
        refq.setParameter("ids", staleImageCacheIds);
        List<Long> existing = refq.getResultList();

        staleImageCacheIds.removeAll(existing);

        if (staleImageCacheIds.isEmpty()) {
            return null;
        }

        sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> fq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        fq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> stale = fq.getResultList();

        logger.debug(String.format("found %s stale images in cache on the primary storage[type:%s], they are about to be cleaned up",
                stale.size(), getPrimaryStorageType()));

        for (ImageCacheVO vo : stale) {
            dbf.getEntityManager().persist(new ImageCacheShadowVO(vo));
            dbf.getEntityManager().remove(vo);
        }

        sql = "select s from ImageCacheShadowVO s";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        return sq.getResultList();
    }

    @Override
    public void managementNodeReady() {
        startGC();
    }
}
