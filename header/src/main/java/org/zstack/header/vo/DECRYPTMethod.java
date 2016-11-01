package org.zstack.header.vo;

import org.apache.commons.codec.binary.Base64;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.zstack.utils.EncryptRSA;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;

/**
 * Created by mingjian.deng on 16/11/1.
 */

@Aspect
@Component
public class DECRYPTMethod {
    private static final CLogger logger = Utils.getLogger(DECRYPTMethod.class);

    @Around("@annotation(org.zstack.header.vo.DECRYPT)")
    public Object decrypt(ProceedingJoinPoint joinPoint) throws Throwable {
        Object returnValue = joinPoint.proceed();
        if(returnValue != null && returnValue.getClass() == String.class)
            return decrypt((String)returnValue);
        else
            return returnValue;
    }

    private Object decrypt(String password) throws NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
            NoSuchPaddingException, IOException, ClassNotFoundException {
        EncryptRSA rsa = new EncryptRSA();
        RSAPrivateKey privateKey = rsa.getPrivateKey();
        byte[] srcBytes = password.getBytes("utf-8");
        byte[] desBytes = rsa.decrypt(privateKey, Base64.decodeBase64(srcBytes));
        return new String(desBytes, "utf-8");
    }

}
