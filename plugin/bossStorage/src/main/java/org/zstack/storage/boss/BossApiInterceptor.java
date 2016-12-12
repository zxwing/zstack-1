package org.zstack.storage.boss;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.storage.boss.backup.*;
import org.zstack.storage.boss.primary.*;

/**
 * Created by XXPS-PC1 on 2016/10/27.
 */
public class BossApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(BossApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddBossBackupStorageMsg) {
            validate((APIAddBossBackupStorageMsg) msg);
        } else if (msg instanceof APIAddBossPrimaryStorageMsg) {
            validate((APIAddBossPrimaryStorageMsg) msg);
        }

        return msg;
    }

    public void validate(APIAddBossBackupStorageMsg msg){
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "poolName can be null but cannot be an empty string"
            ));
        }
    }

    public  void validate(APIAddBossPrimaryStorageMsg msg){
        if (msg.getDataVolumePoolName() != null && msg.getDataVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "dataVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getRootVolumePoolName() != null && msg.getRootVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "rootVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getImageCachePoolName() != null && msg.getImageCachePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "imageCachePoolName can be null but cannot be an empty string"
            ));
        }
    }




}
