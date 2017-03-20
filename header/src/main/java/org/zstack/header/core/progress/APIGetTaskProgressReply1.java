package org.zstack.header.core.progress;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
/**
 * Created by mingjian.deng on 16/12/8.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetTaskProgressReply1 extends APIReply {
    private String progress;
    private String resourceUuid;
    private String processType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getProcessType() {
        return processType;
    }

    public void setProcessType(String processType) {
        this.processType = processType;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
 
    public static APIGetTaskProgressReply1 __example__() {
        APIGetTaskProgressReply1 reply = new APIGetTaskProgressReply1();
        reply.setProcessType("AddImage");
        reply.setResourceUuid("f16661c706ae403883f5e4cca6f1f3f4");
        reply.setProgress("99");
        reply.setLastOpDate(new Timestamp(2017, 1, 22, 11, 26, 30, 5000));
        reply.setCreateDate(new Timestamp(2017, 1, 22, 11, 26, 30, 5000));
        return reply;
    }

}
