package org.zstack.header.vo;

/**
 * Created by xing5 on 2017/4/20.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ResourceVO.class)
public class ResourceVO_ {
    public static volatile SingularAttribute<ResourceVO, String> uuid;
    public static volatile SingularAttribute<ResourceVO, String> name;
}
