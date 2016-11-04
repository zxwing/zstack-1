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

    }

    public  void validate(APIAddBossPrimaryStorageMsg msg){

    }




}
