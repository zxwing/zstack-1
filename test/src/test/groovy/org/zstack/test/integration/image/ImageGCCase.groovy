package org.zstack.test.integration.image

import org.zstack.header.image.ImageVO
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.ImageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/5.
 */
class ImageGCCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ImageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneSftpEnv
    }

    void testImageGCWhenBackupStorageDisconnect() {
        ImageInventory image = (env.specByName("image") as ImageSpec).inventory

        env.simulator(SftpBackupStorageConstant.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        assert !dbIsExists(image.uuid, ImageVO.class)
    }

    @Override
    void test() {
        env.create {
            testImageGCWhenBackupStorageDisconnect()
        }
    }
}
