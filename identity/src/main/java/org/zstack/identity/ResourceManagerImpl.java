package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.AbstractEntityLifeCycleCallback;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.Resource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/4/24.
 */
public class ResourceManagerImpl extends AbstractEntityLifeCycleCallback implements ResourceManager, Component {
    CLogger logger = Utils.getLogger(getClass());

    @Autowired
    private DatabaseFacade dbf;

    private class ResourceInfo {
        Field resourceUuidField;
    }

    private Map<Class, ResourceInfo> infoMap = new HashMap<>();

    @Override
    public boolean start() {
        try {
            List<Class> classes = Platform.getReflections().getTypesAnnotatedWith(Resource.class).stream()
                    .filter(it -> it.isAnnotationPresent(Resource.class)).collect(Collectors.toList());

            for (Class clz : classes) {
                ResourceInfo info = new ResourceInfo();
                String fieldName = StringUtils.removeEnd(StringUtils.uncapitalize(clz.getSimpleName()), "VO") + "Uuid";
                info.resourceUuidField = ResourceVO.class.getDeclaredField(fieldName);
                info.resourceUuidField.setAccessible(true);
                infoMap.put(clz, info);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        dbf.installEntityLifeCycleCallback(null, EntityEvent.PRE_PERSIST, this);
        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_PERSIST, new AbstractEntityLifeCycleCallback() {
            @Override
            @Transactional
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                ResourceInfo info = infoMap.get(o.getClass());
                if (info == null) {
                    return;
                }

                try {
                    String uuid = (String) getPrimaryKeyValue(o);
                    ResourceVO vo = dbf.getEntityManager().find(ResourceVO.class, uuid);
                    info.resourceUuidField.set(vo, uuid);

                    dbf.getEntityManager().merge(vo);
                    vo = dbf.getEntityManager().find(ResourceVO.class, uuid);

                    logger.debug(String.format("yyyyyyyyyyyyyyyyyyyyyyyyy %s %s %s", o.getClass().getSimpleName(), uuid, vo.getVmNicUuid()));
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @Transactional
    public void entityLifeCycleEvent(EntityEvent evt, Object o) {
        try {
            ResourceInfo info = infoMap.get(o.getClass());
            if (info == null) {
                return;
            }

            String uuid = (String) getPrimaryKeyValue(o);
            ResourceVO vo = new ResourceVO();
            vo.setUuid(uuid);
            vo.setType(o.getClass().getSimpleName());

            //info.resourceUuidField.set(vo, uuid);

            dbf.getEntityManager().persist(vo);

            logger.debug(String.format("Xxxxxxxxxxxxxxxxxxxxxxxx %s %s", o.getClass().getSimpleName(), uuid));
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
