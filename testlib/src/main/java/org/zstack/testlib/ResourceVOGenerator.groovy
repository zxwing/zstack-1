package org.zstack.testlib

import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.header.vo.EO
import org.zstack.header.vo.Resource

/**
 * Created by xing5 on 2017/4/19.
 */
class ResourceVOGenerator {

    void generate(String outputDir) {
        File dir = new File([outputDir, "zstack-resourcevo"].join("/"))
        dir.mkdirs()

        Set resourceVOs = Platform.reflections.getSubTypesOf(Resource.class)
        String resourceVOText = writeResourceVOText(resourceVOs)
        String sql = writeSqlText(resourceVOs)

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

    private String writeSqlText(Set<Class> voClasses) {
        List<String> fkeys = voClasses.collect {
            return "ALTER TABLE ResourceVO ADD CONSTRAINT fkResourceVO${it.simpleName} FOREIGN KEY ${classToColumnName(it)} REFERENCES ${getForeignClass(it).simpleName} (uuid) ON DELETE SET NULL;"
        }

        List<String> inserts = voClasses.collect {
            if (it.hasProperty("name")) {
                return "INSERT INTO ResourceVO (uuid, name, ${classToColumnName(it)}) SELECT t.uuid, t.name, t.uuid FROM ${it.simpleName} t;"
            } else {
                return "INSERT INTO ResourceVO (uuid, ${classToColumnName(it)}) SELECT t.uuid, t.uuid FROM ${it.simpleName} t;"
            }
        }

        return """\
${fkeys.join("\n")}

${inserts.join("\n")}
"""
    }

    private String writeResourceVOText(Set<Class> voClasses) {
        List<String> resourceColumns = voClasses.collect {
            String originName = it.simpleName - "VO"
            String colName = classToColumnName(it)

            Class foreignClass = getForeignClass(it)

            return """\
    @Column
    @ForeignKey(parentEntityClass = ${foreignClass.name}.class, onDeleteAction = ReferenceOption.CASCADE)
    private String $colName

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

public class ResourceVO {

    @Id
    @Column
    private String uuid;
    
    @Column
    private String name;
    
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
    
    ${resourceColumns.join("\n")}
    
}
"""
    }
}
