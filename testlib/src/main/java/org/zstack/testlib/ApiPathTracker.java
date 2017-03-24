package org.zstack.testlib;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.apache.logging.log4j.ThreadContext;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusImpl2;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.rest.RESTFacadeImpl;
import org.zstack.core.workflow.BeforeFlowRunExtensionPoint;
import org.zstack.header.Constants;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/3/24.
 */
public class ApiPathTracker implements BeforeFlowRunExtensionPoint {
    private CloudBus bus;
    private RESTFacade restf;
    private PluginRegistry pluginRgty;

    private String apiId;

    Tracker tracker;

    private enum Type {
        SendMessage,
        ReceiveMessage,
        HttpRPC,
        Flow
    }

    private static class Path {
        Type type;
        String path;
    }

    public abstract class Tracker {
        List<Path> paths = new ArrayList<>();

        public abstract List<String> apiPath();
    }

    public Tracker track() {
        return tracker;
    }

    private String getClassAndMethodNameAndLineNumber(Class clz, String methodName) {
        StackTraceElement[] ss = Thread.currentThread().getStackTrace();
        for (StackTraceElement s : ss) {
            if (s.getClassName().equals(clz.getName()) && s.getMethodName().equals(methodName)) {
                return String.format("%s:%s::%s", s.getFileName(), s.getLineNumber(), s.getMethodName());
            }
        }

        return null;
    }

    public ApiPathTracker(String apiId) {
        this.apiId = apiId;
        bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
        restf = Platform.getComponentLoader().getComponent(RESTFacade.class);

        tracker = new Tracker() {
            @Override
            public List<String> apiPath() {
                return paths.stream().map(p -> String.format("%s %s", p.type, p.path)).collect(Collectors.toList());
            }
        };

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                String cml = getClassAndMethodNameAndLineNumber(CloudBusImpl2.class, "send");
                if (cml != null) {
                    Path p = new Path();
                    p.type = Type.SendMessage;
                    p.path = cml;
                    tracker.paths.add(p);
                }
            }
        });

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            private void record() {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                String cml = getClassAndMethodNameAndLineNumber(RESTFacadeImpl.class, "asyncJsonPost");
                if (cml != null) {
                    Path p = new Path();
                    p.type = Type.HttpRPC;
                    p.path = cml;
                    tracker.paths.add(p);
                }
            }

            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                record();
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                record();
            }
        });

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent)) {
                    return;
                }

                APIEvent aevt = (APIEvent) evt;
                if (!aevt.getApiId().equals(apiId)) {
                    return;
                }

                synchronized (tracker) {
                    tracker.notifyAll();
                }
            }
        });

        pluginRgty.defineDynamicExtension(BeforeFlowRunExtensionPoint.class, this);
    }

    @Override
    public void beforeFlowRun(String flowName, Flow flow, FlowChain chain, Map data) {
        String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

        if (id == null || !id.equals(apiId)) {
            return;
        }

        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get(flow.getClass().getName());
            cc.stopPruning(true);
            CtMethod m = cc.getDeclaredMethod("run");

            Path p = new Path();
            p.type = Type.Flow;
            p.path = String.format("%s:%s[%s]::run()", cc.getClassFile().getSourceFile(), m.getMethodInfo().getLineNumber(0), flowName);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
