package org.zstack.header.vo;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/5/1.
 */
@AutoQuery(replyClass = APIQueryResourceReply.class, inventoryClass = ResourceInventory.class)
@RestRequest(
        path = "/resources",
        optionalPaths = {"/resources/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryResourceReply.class
)
public class APIQueryResourceMsg extends APIQueryMessage {
}
