package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.header.vo.DECRYPT;
import org.zstack.header.vo.ENCRYPT;

/**
 * Created by mingjian.deng on 16/11/2.
 */
public class TestEncrypt {
    private String password;

    @DECRYPT
    public String getPassword() {
        return password;
    }

    public String getPassword(boolean encrypt){
        if(encrypt)
            return getPassword();
        else
            return password;
    }

    @ENCRYPT
    public void setPassword(String password) {
        this.password = password;
    }

    @Test
    public void test(){
        TestEncrypt testEncrypt = new TestEncrypt();
        testEncrypt.setPassword("password");
        Assert.assertNotNull(testEncrypt.getPassword());
        Assert.assertTrue("password" == testEncrypt.getPassword(true));
        Assert.assertFalse("password" == testEncrypt.getPassword(false));
    }
}
