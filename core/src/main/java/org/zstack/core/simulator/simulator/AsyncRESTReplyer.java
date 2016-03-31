package org.zstack.core.simulator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NOTE : only for unit test
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AsyncRESTReplyer {
    @Autowired
    private RESTFacade restf;

    private static Map<Class, BeforeDeliverResponseInterceptor> interceptors = new HashMap<Class, BeforeDeliverResponseInterceptor>();
    private static List<BeforeDeliverResponseInterceptor> globalInteceptors = new ArrayList<BeforeDeliverResponseInterceptor>();

    public static void installBeforeDeliverResponseInterceptor(BeforeDeliverResponseInterceptor ic, Class...classes) {
        if (classes.length == 0) {
            globalInteceptors.add(ic);
        } else {
            for (Class clz : classes) {
                interceptors.put(clz, ic);
            }
        }
    }

    private void callInterceptor(Object rsp) {
        BeforeDeliverResponseInterceptor ic = interceptors.get(rsp.getClass());
        if (ic != null) {
            ic.beforeDeliverResponse(rsp);
        }

        for (BeforeDeliverResponseInterceptor i : globalInteceptors) {
            i.beforeDeliverResponse(rsp);
        }
    }

    public void reply(HttpEntity<String> entity, Object rsp) {
        callInterceptor(rsp);

        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        String rspBody = JSONObjectUtil.toJsonString(rsp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(rspBody.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers);
        restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
    }
}
