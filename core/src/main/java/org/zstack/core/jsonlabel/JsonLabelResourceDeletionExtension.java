package org.zstack.core.jsonlabel;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;

import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/9/14.
 */
public class JsonLabelResourceDeletionExtension implements Component {

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean start() {
        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_REMOVE, new AbstractEntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                if (String.class.isAssignableFrom(getPrimaryKeyField(o).getType())) {
                    SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, getPrimaryKeyValue(o)).hardDelete();
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
