package org.zstack.header.vo;

import org.apache.commons.codec.binary.Base64;
import org.aspectj.lang.ProceedingJoinPoint;
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
import java.security.interfaces.RSAPublicKey;

/**
 * Created by mingjian.deng on 16/11/1.
 * exec TestEncrypt can test this method
 */

@Aspect
@Component
public class EncryptMethod {
    private static final CLogger logger = Utils.getLogger(EncryptMethod.class);
    @Around("@annotation(org.zstack.header.vo.ENCRYPT)")
    public void encrypt(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] parameters = joinPoint.getArgs();
        Object proxy = joinPoint.getThis();
        logger.debug("proxy.getClass is: ");
        logger.debug(proxy.getClass().getName());
        if(parameters.length > 0 && parameters[0].getClass() == String.class){
            parameters[0] = encrypt((String)parameters[0]);
            logger.debug(String.format("encrypted password is: %s", parameters[0]));
            joinPoint.proceed(parameters);
        }
    }

    private String encrypt(String password) throws NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
            NoSuchPaddingException, IOException, ClassNotFoundException {
        EncryptRSA rsa = new EncryptRSA();
        RSAPublicKey publicKey = rsa.getPublicKey();
        byte[] srcBytes = password.getBytes("utf-8");
        byte[] tmp = rsa.encrypt(publicKey, srcBytes);
        byte[] desBytes = Base64.encodeBase64(tmp);
        logger.debug(String.format("encrypt password: %s", new String(desBytes, "utf-8")));
        // for test
        RSAPrivateKey privateKey = rsa.getPrivateKey();
        logger.debug(String.format("encrypt password: %s", new String(rsa.decrypt(privateKey, Base64.decodeBase64(desBytes)), "utf-8")));
        return new String(desBytes, "utf-8");
    }
}
