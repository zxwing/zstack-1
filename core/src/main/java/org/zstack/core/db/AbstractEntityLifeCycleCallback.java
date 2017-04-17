package org.zstack.core.db;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xing5 on 2017/4/18.
 */
public abstract class AbstractEntityLifeCycleCallback implements EntityLifeCycleCallback {
    private static ConcurrentHashMap<Class, Field> primaryKeys = new ConcurrentHashMap<>();

    static {
        Set<Field> pfields = Platform.getReflections().getFieldsAnnotatedWith(Id.class);
        for (Field f : pfields) {
            f.setAccessible(true);
            primaryKeys.put(f.getDeclaringClass(), f);
        }
    }

    protected Object getPrimaryKeyValue(Object entity) {
        Field pfield = primaryKeys.get(entity.getClass());

        if (pfield == null) {
            Class c = entity.getClass().getSuperclass();

            while (c != Object.class) {
                pfield = primaryKeys.get(c);
                if (pfield != null) {
                    break;
                }

                c = c.getSuperclass();
            }

            if (pfield == null) {
                throw new CloudRuntimeException(String.format("no primary key field annotated by @Id found for the class[%s]", entity.getClass()));
            }

            pfield.setAccessible(true);
            primaryKeys.put(entity.getClass(), pfield);
        }

        try {
            return pfield.get(entity);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
