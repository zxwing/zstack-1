package org.zstack.header.configuration;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.Resource;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = DiskOfferingEO.class)
@AutoDeleteTag
@Resource
public class DiskOfferingVO extends DiskOfferingAO {
}
