package org.zstack.header.tag;

import java.lang.annotation.*;

/**
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TagResourceType {
    Class value();
}
