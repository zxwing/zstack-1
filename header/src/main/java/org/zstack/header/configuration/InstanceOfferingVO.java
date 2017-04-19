package org.zstack.header.configuration;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.Resource;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EO(EOClazz = InstanceOfferingEO.class)
@AutoDeleteTag
@Resource
public class InstanceOfferingVO extends InstanceOfferingAO {
    public InstanceOfferingVO() {
    }

    public InstanceOfferingVO(InstanceOfferingVO other) {
        super(other);
    }
}

