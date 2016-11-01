package org.zstack.header.vo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.springframework.stereotype.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by mingjian.deng on 16/11/1.
 */

@Aspect
@Component
public class EncryptMethod {
    private static final CLogger logger = Utils.getLogger(EncryptMethod.class);

    @Around("@annotation(org.zstack.header.vo.ENCRYPT)")
    public void encrypt(ProceedingJoinPoint joinPoint) throws Throwable {
        ENCRYPT en = getAnnotation(joinPoint, ENCRYPT.class);
        Object[] parameters = joinPoint.getArgs();
        logger.debug("encrypt password!");
        logger.debug(String.format("encrypt path is: %s", en.value()));
        if(parameters.length > 0 && parameters[0].getClass() == String.class){
            parameters[0] = encrypt((String)parameters[0]);
            logger.debug(String.format("encrypted password is: %s", parameters[0]));
            joinPoint.proceed(parameters);
        }
    }

    private String encrypt(String password){
        return password;
    }

    private <T extends Annotation> T getAnnotation(ProceedingJoinPoint jp, Class<T> clazz) {
        MethodSignature sign = (MethodSignature) jp.getSignature();
        Method method = sign.getMethod();
        return method.getAnnotation(clazz);
    }
}
