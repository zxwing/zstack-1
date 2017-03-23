package org.zstack.core.progress;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.progress.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;


/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressReportService extends AbstractService implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ProgressReportService.class);
    @Autowired
    private RESTFacade restf;

    @Autowired
    protected ErrorFacade errf;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private CloudBus bus;

    private static class TaskStep {
        int steps;
        Map<String, TaskStep> subTasks = new HashMap<>();
    }

    private Map<String, TaskStep> steps = new ConcurrentHashMap<>();


    private void setThreadContext(ProgressReportCmd cmd) {
        ThreadContext.clearAll();
        if (cmd.getThreadContextMap() != null) {
            ThreadContext.putAll(cmd.getThreadContextMap());
        }
        if (cmd.getThreadContextStack() != null) {
            ThreadContext.setStack(cmd.getThreadContextStack());
        }
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_START_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                //TODO
                logger.debug(String.format("call PROGRESS_START_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                startProcess(cmd);
                return null;
            }
        });

        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_REPORT_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                setThreadContext(cmd);
                taskProgress(TaskType.Progress, cmd.getProgress());

                //TODO
                logger.debug(String.format("call PROGRESS_REPORT_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                //process(cmd);
                return null;
            }
        });

        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_FINISH_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                //TODO
                logger.debug(String.format("call PROGRESS_FINISH_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                finishProcess(cmd);
                return null;
            }
        });

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent)) {
                    return;
                }

                String apiName = ThreadContext.get(Constants.THREAD_CONTEXT_API_NAME);
                if (apiName == null || ThreadContext.get(Constants.THREAD_CONTEXT_PROGRESS_ENABLED) == null) {
                    return;
                }

                String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
                if (apiId == null) {
                    return;
                }

                if (steps.containsKey(apiName)) {
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        TaskStepVO vo = q(TaskStepVO.class).eq(TaskStepVO_.taskName, apiName).find();
                        if (vo == null) {
                            saveToDb();
                        } else {
                            TaskStep step = JSONObjectUtil.toObject(vo.getContent(), TaskStep.class);
                            steps.put(apiName, step);
                        }
                    }

                    private void createSubTaskStep(TaskStep parent, List<TaskProgressInventory> invs) {
                        if (invs.get(0).getType().equals(TaskType.Progress.toString())) {
                            return;
                        }

                        for (TaskProgressInventory inv : invs) {
                            if (inv.getSubTasks() == null || inv.getSubTasks().isEmpty()) {
                                continue;
                            }

                            TaskStep sub = new TaskStep();
                            parent.subTasks.put(inv.getTaskName(), sub);
                            sub.steps = inv.getSubTasks().size();
                            createSubTaskStep(sub, inv.getSubTasks());
                        }
                    }

                    private void saveToDb() {
                        List<TaskProgressInventory> invs = getAllProgress(apiId);
                        if (invs.isEmpty() || invs.get(0).getType().equals(TaskType.Progress.toString())) {
                            steps.put(apiName, null);
                            return;
                        }

                        TaskStep step = new TaskStep();
                        step.steps = invs.size();
                        createSubTaskStep(step, invs);

                        TaskStepVO vo = new TaskStepVO();
                        vo.setContent(JSONObjectUtil.toJsonString(step));
                        vo.setTaskName(apiName);
                        dbf.getEntityManager().persist(vo);

                        steps.put(apiName, step);
                    }
                }.execute();
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void validation(ProgressReportCmd cmd) {
        validationType(cmd.getProcessType());
        validationUuid(cmd.getResourceUuid());
    }

    private void validationType(String processType) {
        if (processType == null || !ProgressConstants.ProgressType.contains(processType) ) {
            logger.warn(String.format("not supported processType: %s", processType));
            throw new OperationFailureException(operr("not supported processType: %s",
                            processType));
        }
    }

    private void validationUuid(String uuid) {
        if (uuid == null) {
            logger.warn("not supported null uuid");
            throw new OperationFailureException(operr("not supported null uuid"));
        }
    }

    private void startProcess(ProgressReportCmd cmd) {
        validation(cmd);
        insertProgress(cmd);
    }

    private void process(ProgressReportCmd cmd) {
        validation(cmd);
        updateProgress(cmd);
    }

    private void finishProcess(ProgressReportCmd cmd) {
        validation(cmd);
        deleteProgress(cmd);
    }

    @Transactional
    private void insertProgress(ProgressReportCmd cmd) {
        logger.debug(String.format("insert progress and it begins, processType is: %s", cmd.getProcessType()));
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        // please notice if there are no conditions that result more than two vo found...
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        if (q.isExists()) {
            logger.warn(String.format("delete records that shouldn't exist...: %s", q.count()));
            q.list().stream().forEach(p -> dbf.remove(p));
        }
        ProgressVO vo = new ProgressVO();
        vo.setProgress(cmd.getProgress() == null? "0":cmd.getProgress());
        vo.setProcessType(cmd.getProcessType());
        vo.setResourceUuid(cmd.getResourceUuid());
        dbf.persistAndRefresh(vo);
    }

    @Transactional
    private void deleteProgress(ProgressReportCmd cmd) {
        logger.debug("delete progress and it's over");
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        // please notice if there are no conditions that result more than two vo found...
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        List<ProgressVO> list = q.list();
        if (list.size() > 0) {
            for (ProgressVO p : list) {
                try {
                    dbf.remove(p);
                } catch (Exception e) {
                    logger.warn("no need delete, it was deleted...");
                }
            }
        }
    }

    @Override
    public void managementNodeReady() {

    }

    @Transactional
    private void updateProgress(ProgressReportCmd cmd) {
        logger.debug(String.format("update progress and during processing, progress is: %s, resource is: %s", cmd.getProgress(), cmd.getResourceUuid()));
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        if (q.isExists()) {
            List<ProgressVO> list = q.list();
            if (list.size() > 0) {
                ProgressVO vo = list.get(list.size() - 1);
                vo.setProgress(cmd.getProgress());
                dbf.updateAndRefresh(vo);
            }
        } else {
            logger.debug(String.format("progress is not existed, insert progress and it begins, processType is: %s", cmd.getProcessType()));
            ProgressVO vo = new ProgressVO();
            vo.setProgress(cmd.getProgress() == null? "0":cmd.getProgress());
            vo.setProcessType(cmd.getProcessType());
            vo.setResourceUuid(cmd.getResourceUuid());
            dbf.persistAndRefresh(vo);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ProgressConstants.SERVICE_ID);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetTaskProgressMsg1) {
            handle((APIGetTaskProgressMsg1) msg);
        } else if (msg instanceof APIGetTaskProgressMsg) {
            handle((APIGetTaskProgressMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private TaskProgressInventory inventory(TaskProgressVO vo) {
        TaskProgressInventory inv = new TaskProgressInventory(vo);
        if (vo.getArguments() == null) {
            inv.setContent(toI18nString(vo.getContent()));
        } else {
            List<String> args = JSONObjectUtil.toCollection(vo.getArguments(), ArrayList.class, String.class);
            inv.setContent(toI18nString(vo.getContent(), args.toArray()));
        }

        return inv;
    }

    @Transactional
    private List<TaskProgressInventory> getAllProgress(String apiId) {
        List<TaskProgressVO> vos = Q.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiId).list();
        if (vos.isEmpty()) {
            return new ArrayList<>();
        }

        List<TaskProgressInventory> invs = vos.stream().map(this::inventory).collect(Collectors.toList());
        Map<String, List<TaskProgressInventory>> map = new HashMap<>();
        String nullKey = "null";
        for (TaskProgressInventory inv : invs) {
            String key = inv.getParentUuid() == null ? nullKey : inv.getParentUuid();
            List<TaskProgressInventory> lst = map.computeIfAbsent(key, k -> new ArrayList<>());
            lst.add(inv);
        }

        // sort by time with DESC
        for (Map.Entry<String, List<TaskProgressInventory>> e : map.entrySet()) {
            e.getValue().sort((o1, o2) -> (int) (o1.getTime() - o2.getTime()));

            for (TaskProgressInventory inv : e.getValue()) {
                if (!inv.getType().equals(TaskType.Progress.toString())) {
                    inv.setCurrentStep(e.getValue().size());
                }
            }
        }

        for (Map.Entry<String, List<TaskProgressInventory>> e : map.entrySet()) {
            if (e.getKey().equals(nullKey)) {
                continue;
            }

            Optional<TaskProgressInventory> opt = invs.stream().filter(it -> e.getKey().equals(it.getTaskUuid())).findAny();
            assert opt.isPresent();
            TaskProgressInventory inv = opt.get();
            inv.setSubTasks(e.getValue());
        }

        invs = map.get(nullKey);
        return invs;
    }

    private void handle(APIGetTaskProgressMsg msg) {
        APIGetTaskProgressReply reply = new APIGetTaskProgressReply();

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (msg.isAll()) {
                    replyAllProgress();
                } else {
                    replyLastProgress();
                }
            }

            private Integer calculateCurrentStep(TaskProgressVO vo) {
                if (vo.getType() == TaskType.Progress) {
                    return null;
                }

                if (vo.getParentUuid() ==  null)  {
                    return Q.New(TaskProgressVO.class).isNull(TaskProgressVO_.parentUuid).count().intValue();
                } else {
                    return Q.New(TaskProgressVO.class).eq(TaskProgressVO_.parentUuid, vo.getParentUuid()).count().intValue();
                }
            }

            private void replyLastProgress() {
                TaskProgressVO vo = Q.New(TaskProgressVO.class)
                        .eq(TaskProgressVO_.apiId, msg.getApiId())
                        .orderBy(TaskProgressVO_.time, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();

                if (vo == null) {
                    reply.setInventories(new ArrayList<>());
                    return;
                }

                TaskProgressInventory inv;
                if (vo.getParentUuid() == null) {
                    inv = inventory(vo);
                    inv.setCurrentStep(calculateCurrentStep(vo));
                    reply.setInventories(asList(inv));
                    return;
                }

                Stack<TaskProgressInventory> invs = new Stack<>();
                inv = inventory(vo);
                inv.setCurrentStep(calculateCurrentStep(vo));
                invs.push(inv);

                while (vo.getParentUuid() != null) {
                    vo = Q.New(TaskProgressVO.class)
                            .eq(TaskProgressVO_.apiId, msg.getApiId())
                            .eq(TaskProgressVO_.taskUuid, vo.getParentUuid())
                            .orderBy(TaskProgressVO_.time, SimpleQuery.Od.DESC)
                            .limit(1)
                            .find();

                    if (vo == null) {
                        break;
                    }

                    inv = inventory(vo);
                    inv.setCurrentStep(calculateCurrentStep(vo));
                    invs.push(inv);
                }

                reply.setInventories(invs);
            }

            private void replyAllProgress() {
                reply.setInventories(getAllProgress(msg.getApiId()));
            }
        }.execute();
        
        setStepsNumber(reply);

        bus.reply(msg, reply);
    }

    private void setStepsNumberForSubTask(TaskProgressInventory parent, TaskStep step) {
        if (parent.getSubTasks() == null || parent.getSubTasks().isEmpty()) {
            return;
        }

        for (TaskProgressInventory inv : parent.getSubTasks()) {
            TaskStep sub = step.subTasks.get(inv.getTaskName());
            if (sub != null) {
                inv.setTotalSteps(sub.steps);
            }

            setStepsNumberForSubTask(inv, sub);
        }
    }

    private void setStepsNumber(APIGetTaskProgressReply reply) {
        for (TaskProgressInventory inv : reply.getInventories()) {
            TaskStep step = steps.get(inv.getTaskName());
            if (step == null) {
                // no step count for this task, directly out
                return;
            }

            inv.setTotalSteps(step.steps);
            setStepsNumberForSubTask(inv, step);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(APIGetTaskProgressMsg1 msg) {
        APIGetTaskProgressReply1 reply = new APIGetTaskProgressReply1();
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, msg.getResourceUuid());
        if (msg.getProcessType() != null) {
            q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, msg.getProcessType());
        }
        q.orderBy(ProgressVO_.lastOpDate, SimpleQuery.Od.ASC);
        List<ProgressVO> vos = q.list();
        if (q.list().size() == 0) {
            reply.setSuccess(true);
        } else {
            ProgressVO vo = vos.get(vos.size() - 1);
            reply.setProgress(vo.getProgress());
            reply.setCreateDate(vo.getCreateDate());
            reply.setLastOpDate(vo.getLastOpDate());
            reply.setProcessType(vo.getProcessType());
            reply.setResourceUuid(vo.getResourceUuid());
        }
        bus.reply(msg, reply);
    }

    private static String getTaskUuid() {
        return ThreadContext.peek();
    }

    private static String getParentUuid() {
        if (ThreadContext.getImmutableStack().isEmpty()) {
            return null;
        }

        if (ThreadContext.getImmutableStack().size() == 1) {
            String uuid = ThreadContext.get(Constants.THREAD_CONTEXT_API);
            assert uuid != null;
            return uuid;
        }

        List<String> lst = ThreadContext.getImmutableStack().asList();
        return lst.get(lst.size()-2);
    }

    public static void createSubTaskProgress(String fmt, Object...args) {
        if (!ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            if (args != null) {
                logger.warn(String.format("no task uuid found for:" + fmt, args));
            } else {
                logger.warn("no task uuid found for:" + fmt);
            }
            return;
        }

        ThreadContext.put(Constants.THREAD_CONTEXT_PROGRESS_ENABLED, "true");

        String parentUuid = getParentUuid();
        String taskUuid = Platform.getUuid();
        ThreadContext.push(taskUuid);
        ThreadContext.push(Platform.getUuid());

        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(ThreadContext.get(Constants.THREAD_CONTEXT_API));
        vo.setTaskUuid(taskUuid);
        vo.setParentUuid(parentUuid);
        vo.setContent(fmt);
        if (args != null) {
            vo.setArguments(JSONObjectUtil.toJsonString(args));
        }
        vo.setType(TaskType.Task);
        vo.setTime(System.currentTimeMillis());
        vo.setManagementUuid(Platform.getManagementServerId());
        vo.setTaskName(vo.getContent());

        Platform.getComponentLoader().getComponent(DatabaseFacade.class).persist(vo);
    }

    private static void taskProgress(TaskType type, String fmt, Object...args) {
        if (!ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            if (args != null) {
                logger.warn(String.format("no task uuid found for:" + fmt, args));
            } else {
                logger.warn("no task uuid found for:" + fmt);
            }
            return;
        }

        ThreadContext.put(Constants.THREAD_CONTEXT_PROGRESS_ENABLED, "true");

        String taskUuid = getTaskUuid();
        if (taskUuid.isEmpty()) {
            taskUuid = Platform.getUuid();
        }

        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(ThreadContext.get(Constants.THREAD_CONTEXT_API));
        vo.setTaskUuid(taskUuid);
        vo.setParentUuid(getParentUuid());
        vo.setContent(fmt);
        if (args != null) {
            vo.setArguments(JSONObjectUtil.toJsonString(args));
        }
        vo.setType(type);
        vo.setTime(System.currentTimeMillis());
        vo.setManagementUuid(Platform.getManagementServerId());
        vo.setTaskName(ThreadContext.get(Constants.THREAD_CONTEXT_API_NAME));

        Platform.getComponentLoader().getComponent(DatabaseFacade.class).persist(vo);
    }

    public static void taskProgress(String fmt, Object...args) {
        taskProgress(TaskType.Task, fmt, args);
    }
}
