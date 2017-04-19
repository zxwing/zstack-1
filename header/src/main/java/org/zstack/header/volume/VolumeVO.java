package org.zstack.header.volume;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.Resource;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
@AutoDeleteTag
public class VolumeVO extends VolumeAO implements Resource {

}
