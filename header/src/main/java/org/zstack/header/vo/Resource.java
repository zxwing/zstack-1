package org.zstack.header.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2017/4/19.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public  @interface Resource {
    String uuid() default "uuid";
    String name() default "name";
}
