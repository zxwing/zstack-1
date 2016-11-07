package org.zstack.storage.boss.backup;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
import org.zstack.storage.boss.BossConstants;

/**
 * Created by XXPS-PC1 on 2016/10/28.
 */
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
public class APIAddBossBackupStorageMsg extends APIAddBackupStorageMsg {
    @APIParam(required = false, maxLength = 255)
    private String poolName;

    public String getUrl() {
        return "not used";
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public String getType() {
        return BossConstants.BOSS_BACKUP_STORAGE_TYPE;
    }
}
