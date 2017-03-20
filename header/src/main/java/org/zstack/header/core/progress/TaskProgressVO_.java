package org.zstack.header.core.progress;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by xing5 on 2017/3/20.
 */
@StaticMetamodel(TaskProgressVO.class)
public class TaskProgressVO_ {
    public static volatile SingularAttribute<Long, Long> id;
    public static volatile SingularAttribute<Long, String> taskUuid;
    public static volatile SingularAttribute<Long, String> apiId;
    public static volatile SingularAttribute<Long, String> parentUuid;
    public static volatile SingularAttribute<Long, TaskType> type;
    public static volatile SingularAttribute<Long, String> content;
    public static volatile SingularAttribute<Long, String> arguments;
    public static volatile SingularAttribute<Long, String> managementUuid;
    public static volatile SingularAttribute<Long, Long> time;
}
