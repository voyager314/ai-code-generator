package com.yzy.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzy.ai.AiCodeGeneratorFacade;
import com.yzy.ai.AiRoutingService;
import com.yzy.ai.CodingAgentService;
import com.yzy.ai.CodingAgentServiceFactory;
import com.yzy.ai.WorkFlow.model.QualityResult;
import com.yzy.ai.approval.ApprovalService;
import com.yzy.ai.handler.StreamHandlerExecutor;
import com.yzy.ai.model.*;
import com.yzy.ai.reflection.AgentReflectionProperties;
import com.yzy.ai.reflection.AgentReflectionService;
import com.yzy.ai.tools.WorkspaceResolver;
import com.yzy.common.AppConstant;
import com.yzy.dto.AppAddRequest;
import com.yzy.dto.AppQueryRequest;
import com.yzy.dto.AppStarQueryRequest;
import com.yzy.dto.AppUpdateRequest;
import com.yzy.entity.App;
import com.yzy.entity.User;
import com.yzy.enums.MessageTypeEnum;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.manager.AliOSSManager;
import com.yzy.mapper.AppMapper;
import com.yzy.monitor.MonitorContext;
import com.yzy.monitor.MonitorContextHolder;
import com.yzy.service.AppService;
import com.yzy.service.ChatHistoryService;
import com.yzy.util.WebScreenShotUtil;
import com.yzy.vo.AppDetailVO;
import com.yzy.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * 应用 服务层实现。
 *
 * @author yzy
 * @since 2026-03-12
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private StreamHandlerExecutor streamHandlerExecutor;

    @Autowired
    private AliOSSManager aliOSSManager;

    @Autowired
    private AiRoutingService aiRoutingService;

    @Autowired
    private CodingAgentServiceFactory codingAgentServiceFactory;

    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private AgentReflectionService agentReflectionService;

    @Autowired
    private AgentReflectionProperties reflectionProperties;

    @Value("${code.deploy-host}")
    private String DEPLOY_HOST;

    @Override
    public Long createApp(AppAddRequest request) {
        // 获取当前用户ID
        Long userId = getCurrentUserId();

        // 创建应用实体
        App app = App.builder()
                .appName(request.getAppName())
                .cover(request.getCover())
                .initPrompt(request.getInitPrompt())
                //替换为智能路由服务
                .codeGenType(aiRoutingService.aiRoutingService(request.getInitPrompt()).getValue())
                .userId(userId)
                .priority(0)
                .build();

        // 保存应用
        save(app);
        return app.getId();
    }

    @Override
    public Boolean updateApp(Long id, AppUpdateRequest request) {
        // 获取当前用户ID
        Long userId = getCurrentUserId();

        // 查询应用
        App app = getById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }

        // 验证用户只能修改自己的应用
        if (!app.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此应用");
        }

        // 更新应用信息
        if (request.getAppName() != null) {
            app.setAppName(request.getAppName());
        }
        if (request.getCover() != null) {
            app.setCover(request.getCover());
        }
        if (request.getPriority() != null) {
            app.setPriority(request.getPriority());
        }

        return updateById(app);
    }

    @Override
    public Boolean deleteApp(Long id) {
        // 获取当前用户ID
        Long userId = getCurrentUserId();

        // 查询应用
        App app = getById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }

        // 验证用户只能删除自己的应用
        if (!app.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此应用");
        }

        // 删除应用的对话历史
        Boolean b = chatHistoryService.deleteChatHistoryByAppId(id);
        if(!b)log.error("对话历史删除失败！");

        return removeById(id);
    }

    @Override
    public AppDetailVO getAppDetail(Long id) {
        App app = getById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }

        // 转换为VO
        AppDetailVO vo = new AppDetailVO();
        BeanUtil.copyProperties(app, vo);
        return vo;
    }

    @Override
    public Page<AppVO> getMyAppList(AppQueryRequest request) {
        Long userId = getCurrentUserId();

        // 构建查询条件
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("userId", userId)
                .eq("isDelete", 0);

        if (request.getAppName() != null && !request.getAppName().isEmpty()) {
            queryWrapper.like("appName", request.getAppName());
        }

        // 设置分页参数（用户分页每页最多20条）
        int pageSize = Math.min(request.getPageSize(), 20);
        int pageNum = request.getPageNum();

        // 执行分页查询
        Page<App> appPage = page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> voList = getAppVOList(appPage.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Page<AppVO> getStarAppList(AppStarQueryRequest request) {
        // 构建查询条件
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("isDelete", 0);

        if (request.getAppName() != null && !request.getAppName().isEmpty()) {
            queryWrapper.like("appName", request.getAppName());
        }

        // 查询有优先级的应用（精选）
        if (request.getPriority() != null) {
            queryWrapper.ge("priority", request.getPriority());
        } else {
            queryWrapper.ge("priority", 0);
        }

        // 设置分页参数（精选应用每页最多20条）
        int pageSize = Math.min(request.getPageSize(), 20);
        int pageNum = request.getPageNum();

        // 执行分页查询
        Page<App> appPage = page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> voList = getAppVOList(appPage.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Boolean adminUpdateApp(Long id, AppUpdateRequest request) {
        // 查询应用
        App app = getById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }

        // 更新应用信息
        if (request.getAppName() != null) {
            app.setAppName(request.getAppName());
        }
        if (request.getCover() != null) {
            app.setCover(request.getCover());
        }
        if (request.getPriority() != null) {
            app.setPriority(request.getPriority());
        }

        return updateById(app);
    }

    @Override
    public Boolean adminDeleteApp(Long id) {
        // 删除应用的对话历史
        Boolean b = chatHistoryService.deleteChatHistoryByAppId(id);
        if(!b)log.error("对话历史删除失败！");//容错，即使对话删除异常也不阻止app删除
        return removeById(id);
    }

    @Override
    public Page<AppVO> adminGetAppList(AppQueryRequest request) {
        // 构建查询条件
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("isDelete", 0);

        // 支持除时间外的任何字段查询
        if (request.getId() != null) {
            queryWrapper.eq("id", request.getId());
        }
        if (request.getAppName() != null && !request.getAppName().isEmpty()) {
            queryWrapper.like("appName", request.getAppName());
        }
        if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }
        if (request.getPriority() != null) {
            queryWrapper.eq("priority", request.getPriority());
        }
        if (request.getCodeGenType() != null && !request.getCodeGenType().isEmpty()) {
            queryWrapper.eq("codeGenType", request.getCodeGenType());
        }

        // 设置分页参数（管理员分页数量不限）
        int pageSize = request.getPageSize();
        int pageNum = request.getPageNum();

        // 执行分页查询
        Page<App> appPage = page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> voPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> voList = getAppVOList(appPage.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        User user = (User) request.getSession().getAttribute("USER_LOGIN_STATE");
        Long userId = user.getId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    /**
     * 将App列表转换为AppVO列表
     */
    private List<AppVO> getAppVOList(List<App> appList) {
        if (appList == null || appList.isEmpty()) {
            return List.of();
        }

        List<AppVO> voList = new ArrayList<>(appList.size());
        for (App app : appList) {
            AppVO vo = new AppVO();
            BeanUtil.copyProperties(app, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String msg, User user,boolean agent) {
        App app = getById(appId);
        ThrowUtil.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //设置监控上下文
        MonitorContextHolder.setContext(MonitorContext.builder()
                        .appId(appId+"")
                        .userId(user.getId()+"")
                .build());
        //保存用户对话历史
        chatHistoryService.saveChatHistory(appId,user.getId(),msg, MessageTypeEnum.USER.getValue());

        if (agent) {
            return startAgentMode(appId, msg, user);
        }

        //非agent模式：走原有代码生成路径
        Flux<String> origin = aiCodeGeneratorFacade.genAndSaveCode(msg, CodeGenTypeEnum.getEnumByValue(app.getCodeGenType()), app.getId());
        return streamHandlerExecutor
                .handle(origin,appId,user,chatHistoryService,CodeGenTypeEnum.getEnumByValue(app.getCodeGenType()))
                .doFinally(signalType -> MonitorContextHolder.removeContext());
    }

    /**
     * 启动 Agent 模式：
     * 1. 创建隔离的工作空间目录 agent_xxx
     * 2. 获取缓存的 CodingAgentService 实例
     * 3. 将 TokenStream 转为 Flux 并注册 sink 到 ApprovalService（启用 HITL）
     * 4. 通过 AgentStreamHandler 将事件转为前端可渲染的 AgentEvent JSON
     */
    private Flux<String> startAgentMode(Long appId, String msg, User user) {
        String workspacePath = AppConstant.OUTPUT_DIR + File.separator + "agent_" + appId;
        new File(workspacePath).mkdirs();
        workspaceResolver.registerWorkspace(appId, workspacePath);

        CodingAgentService agentService = codingAgentServiceFactory.getService(appId);

        Flux<String> origin;
        if (reflectionProperties.isEnabled()) {
            origin = processAgentWithReflection(appId, agentService, msg);
        } else {
            origin = processAgentTokenStream(appId, agentService, msg);
        }

        return streamHandlerExecutor
                .handleAgent(origin, appId, user, chatHistoryService)
                .doFinally(signalType -> {
                    approvalService.removeSink(appId);
                    MonitorContextHolder.removeContext();
                });
    }

    /**
     * 带 reflection 循环的 Agent 执行。
     * 使用单个 Flux.create 管理整个生命周期，避免 concat 多 sink 的复杂度。
     */
    private Flux<String> processAgentWithReflection(Long appId, CodingAgentService agentService, String msg) {
        int maxRetries = reflectionProperties.getMaxRetries();

        return Flux.create(sink -> {
            approvalService.registerSink(appId, sink);

            Schedulers.boundedElastic().schedule(() -> {
                try {
                    runAgentRound(agentService, appId, msg, sink);

                    for (int round = 1; round <= maxRetries; round++) {
                        sink.next(JSONUtil.toJsonStr(AgentEvent.reflectionStarted()));

                        QualityResult result = agentReflectionService.reflect(appId);

                        if (result.getIsValid()) {
                            sink.next(JSONUtil.toJsonStr(AgentEvent.reflectionResult(true, "代码质量检查通过")));
                            break;
                        }

                        String errors = String.join("\n", result.getErrors());
                        String suggestions = String.join("\n", result.getSuggestions());
                        sink.next(JSONUtil.toJsonStr(AgentEvent.reflectionResult(false,
                                "问题：\n" + errors + "\n建议：\n" + suggestions)));

                        if (round < maxRetries) {
                            sink.next(JSONUtil.toJsonStr(AgentEvent.reflectionRetry(round, maxRetries)));
                            String fixPrompt = agentReflectionService.buildFixPrompt(result, msg);
                            runAgentRound(agentService, appId, fixPrompt, sink);
                        }
                    }

                    sink.complete();
                } catch (Exception e) {
                    log.error("Agent reflection loop error, appId={}", appId, e);
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 执行一轮 Agent TokenStream 并阻塞等待完成。
     * 通过 CompletableFuture 将异步 TokenStream 转为同步等待。
     */
    private void runAgentRound(CodingAgentService agentService, Long appId,
                               String prompt, FluxSink<String> sink) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        agentService.chat(appId, prompt)
                .onPartialResponse(partial ->
                        sink.next(JSONUtil.toJsonStr(new AiResponseMessage(partial))))
                .beforeToolExecution(before ->
                        sink.next(JSONUtil.toJsonStr(new ToolRequestMessage(before.request()))))
                .onToolExecuted(toolExec ->
                        sink.next(JSONUtil.toJsonStr(new ToolExecutedMessage(toolExec))))
                .onCompleteResponse(response -> future.complete(null))
                .onError(error -> future.completeExceptionally(error))
                .start();

        future.join();
    }

    /**
     * 不带 reflection 的原始 Agent 流处理（reflection 关闭时使用）。
     */
    private Flux<String> processAgentTokenStream(Long appId, CodingAgentService agentService, String msg) {
        return Flux.create(sink -> {
            approvalService.registerSink(appId, sink);

            agentService.chat(appId, msg)
                    .onPartialResponse(partialResponse -> {
                        AiResponseMessage aiMsg = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiMsg));
                    })
                    .beforeToolExecution(beforeToolExecution -> {
                        ToolRequestMessage toolMsg = new ToolRequestMessage(beforeToolExecution.request());
                        sink.next(JSONUtil.toJsonStr(toolMsg));
                    })
                    .onToolExecuted(toolExecution -> {
                        ToolExecutedMessage toolMsg = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolMsg));
                    })
                    .onCompleteResponse(response -> sink.complete())
                    .onError(error -> {
                        log.error("Agent TokenStream error", error);
                        sink.error(error);
                    })
                    .start();
        });
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        App app = getById(appId);
        ThrowUtil.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在！");
        String codeGenType = app.getCodeGenType();
        String deployKey = app.getDeployKey();
        if(StrUtil.isBlank(deployKey)) {
            //检查是否已有delpoyKey，没有则生成6位唯一key
            deployKey= RandomUtil.randomString(6);
        }
        String outputPath;
        if(!codeGenType.equals("vue_project")){
            outputPath=AppConstant.OUTPUT_DIR + File.separator + String.format("%s_%s", codeGenType, appId);
        }else {
            outputPath=AppConstant.OUTPUT_DIR + File.separator + String.format("%s_%s", "vue_project", appId);
        }
        File outputFile = new File(outputPath);
        if (!outputFile.exists() || !outputFile.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用未生成，请先生成代码！");
        }
        String deployPath=AppConstant.DEPLOY_DIR + File.separator + String.format("%s_%s", codeGenType, deployKey);
        FileUtil.copyContent(outputFile, new File(deployPath), true);
        App update = new App();
        update.setId(appId);
        update.setDeployedTime(LocalDateTime.now());
        update.setDeployKey(deployKey);
        String webUrl=String.format("%s/%s_%s/",DEPLOY_HOST,app.getCodeGenType(),deployKey);
        new Thread(() -> {
            //异步获取网页截图并上传至OSS
            //这里为了保证兼容性，暂时不用java21的虚拟线程
            String filePath = WebScreenShotUtil.getScreenShot(webUrl);
            String coverUrl = aliOSSManager.upload(filePath);
            update.setCover(coverUrl);
            boolean b = updateById(update);
            ThrowUtil.throwIf(!b, ErrorCode.OPERATION_ERROR, "应用部署失败！");
        }).start();
        //提供可访问的url
        return webUrl;
    }
}
