package org.zstack.testlib

import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.header.vo.EO
import org.zstack.header.vo.Resource
import org.zstack.utils.FieldUtils

import javax.persistence.Column

/**
 * Created by xing5 on 2017/4/19.
 */
class ResourceVOGenerator {

    void generate(String outputDir) {
        File dir = new File([outputDir, "zstack-resourcevo"].join("/"))
        dir.mkdirs()

        Set<Class> resourceVOs = Platform.reflections.getTypesAnnotatedWith(Resource.class)
        resourceVOs = resourceVOs.findAll { return it.isAnnotationPresent(Resource.class) }

        String resourceVOText = writeResourceVOText(resourceVOs)
        String sql = writeSqlText(resourceVOs as List)

        new File([dir.absolutePath, "ResourceVO.java"].join("/")).write(resourceVOText)
        new File([dir.absolutePath, "sql.sql"].join("/")).write(sql)
    }

    private String classToColumnName(Class c) {
        String originName = c.simpleName - "VO"
        String name = StringUtils.uncapitalize(originName)
        return name + "Uuid"
    }

    private Class getForeignClass(Class it) {
        EO eo = it.getAnnotation(EO.class)
        return eo == null ? it : eo.EOClazz()
    }

    private String writeSqlText(List<Class> voClasses) {
        voClasses.sort(new Comparator<Class>() {
            @Override
            int compare(Class o1, Class o2) {
                return o1.simpleName <=> o2.simpleName
            }
        })

        List<String> fkeys = voClasses.collect {
            return "ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVO${it.simpleName} FOREIGN KEY (${classToColumnName(it)}) REFERENCES ${getForeignClass(it).simpleName} (uuid) ON DELETE CASCADE;"
        }

        List<String> inserts = voClasses.collect {
            Resource at = it.getAnnotation(Resource.class)

            if (FieldUtils.hasField(at.name(), it)) {
                return "INSERT INTO ResourceVO (uuid, name, ${classToColumnName(it)}, type) SELECT t.uuid, t.${at.name()}, t.uuid, \"${it.simpleName}\" FROM ${it.simpleName} t;"
            } else {
                return "INSERT INTO ResourceVO (uuid, ${classToColumnName(it)}, type) SELECT t.uuid, t.uuid, \"${it.simpleName}\" FROM ${it.simpleName} t;"
            }
        }

        List<String> columns = voClasses.collect {
            return "    `${classToColumnName(it)}` varchar(32) DEFAULT NULL,"
        }

        String table = """\
CREATE TABLE `ResourceVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `type` varchar(255) DEFAULT NULL,
${ columns.join("\n") }
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

"""
        List<String> addColumns = voClasses.collect {
            return "ALTER TABLE `zstack`.`ResourceVO` ADD COLUMN `${classToColumnName(it)}` varchar(32) DEFAULT NULL;"
        }

        return """\
${table}

${fkeys.join("\n")}

${inserts.join("\n")}

${addColumns.join("\n")}
"""
    }

    private String writeResourceVOText(Set<Class> voClasses) {
        List<String> resourceColumns = voClasses.collect {
            String originName = it.simpleName - "VO"
            String colName = classToColumnName(it)

            return """\
    @Column
    private String $colName;

    public String get${originName}Uuid() {
        return $colName;
    }
    
    public void set${originName}Uuid(String v) {
        $colName = v;
    }
"""
        }

        return """\
package org.zstack.header.vo;

import javax.persistence.*;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

@Entity
@Table
public class ResourceVO {

    @Id
    @Column
    private String uuid;
    
    @Column
    private String name;
    
    @Column
    private String type;
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String v) {
        uuid = v;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String v) {
        name = v;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String v) {
        type = v;
    }
    
${resourceColumns.join("\n")}
    
}
"""
    }
}
