package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.AbstractEntityLifeCycleCallback;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.Resource;
import org.zstack.header.vo.ResourceVO;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/4/24.
 */
public class ResourceManagerImpl extends AbstractEntityLifeCycleCallback implements ResourceManager, Component {

    @Autowired
    private DatabaseFacade dbf;

    private class ResourceInfo {
        Field resourceUuidField;
    }

    private Map<Class, ResourceInfo> infoMap = new HashMap<>();

    @Override
    public boolean start() {
        try {
            for (Class clz : Platform.getReflections().getSubTypesOf(Resource.class)) {
                ResourceInfo info = new ResourceInfo();
                String fieldName = StringUtils.uncapitalize(clz.getSimpleName()) + "Uuid";
                info.resourceUuidField = ResourceVO.class.getDeclaredField(fieldName);
                infoMap.put(clz, info);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_PERSIST, this);

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void entityLifeCycleEvent(EntityEvent evt, Object o) {
        try {
            ResourceInfo info = infoMap.get(o.getClass());
            if (info == null) {
                return;
            }

            String uuid = (String) getPrimaryKeyValue(o);
            ResourceVO vo = new ResourceVO();
            vo.setUuid(uuid);

            info.resourceUuidField.set(vo, uuid);

            dbf.getEntityManager().persist(vo);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
