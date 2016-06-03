package org.zstack.core.logging;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Date;

import static org.zstack.core.Platform._;

/**
 * Created by xing5 on 2016/5/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Log {
    @Autowired
    private LogBackend bkd;

    public static class Content {
        public LogLevel level;
        public String text;
        public String resourceUuid;
        public String uuid;
        public long dateInLong;
        public Date date;
        public Object opaque;
    }

    private Content content;

    public Log() {
        this(null);
    }

    public Log(String resourceUuid) {
        content = new Content();
        content.resourceUuid = resourceUuid;
        content.uuid = Generators.timeBasedGenerator().generate().toString().replace("-", "");
        content.dateInLong = System.currentTimeMillis();
        content.date = new Date(content.dateInLong);
        content.level = LogLevel.INFO;
    }

    public Log setLevel(LogLevel level) {
        content.level = level;
        return this;
    }

    public LogLevel getLevel() {
        return content.level;
    }

    public long getDateInLong() {
        return content.dateInLong;
    }

    public Date getDate() {
        return content.date;
    }

    public String getUuid() {
        return content.uuid;
    }

    public String getResourceUuid() {
        return content.resourceUuid;
    }

    public Log setText(String label, Object...args) {
        content.text = _(label, args);
        return this;
    }

    public String getText() {
        return content.text;
    }

    public Content getContent() {
        return content;
    }

    public Log setOpaque(Object opaque) {
        content.opaque = opaque;
        return this;
    }

    public Object getOpaque() {
        return content.opaque;
    }

    public Log log(String label, Object...args) {
        setText(label, args).write();
        return this;
    }

    public void write() {
        bkd.write(this);
    }
}
