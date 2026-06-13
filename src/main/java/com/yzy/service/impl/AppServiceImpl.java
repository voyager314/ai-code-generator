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
import com.yzy.ai.tools.PackageManagerDetector;
import com.yzy.ai.tools.WorkspaceResolver;
import com.yzy.common.AppConstant;
import com.yzy.dto.*;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


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

    @Autowired
    private PackageManagerDetector packageManagerDetector;

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
                //智能路由服务
                //.codeGenType(aiRoutingService.aiRoutingService(request.getInitPrompt()).getValue())
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

        try {
            future.join();
        } catch (Exception e) {
            Throwable root=e;
            while (root.getCause() != null) {root=root.getCause();}
            if (root instanceof com.fasterxml.jackson.core.JsonParseException) {
                // LLM 生成了格式错误的工具调用参数，降级处理而非崩溃。
                // 必须先清理记忆：LangChain4j 已将 AiMessage(tool_calls) 写入 Redis，
                // 但因参数解析失败导致 ToolExecutionResultMessage 未写入，形成非法消息序列。
                // 若不清理，下一轮调用 API 时会被模型以 "insufficient tool messages" 拒绝。
                log.warn("LLM 返回的工具调用参数 JSON 格式错误，appId={}，清理孤立工具调用后重试", appId, root);
                codingAgentServiceFactory.sanitizeMemory(appId);
                sink.next(JSONUtil.toJsonStr(
                        AgentEvent.error("AI 生成的工具调用参数格式异常，将自动重试...")));
                // 不抛出异常 → 让 reflection 循环继续重试
            } else {
                throw e;  // 其他错误仍然向上传播
            }
        }
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
        String deployKey = StrUtil.isBlank(app.getDeployKey()) ? RandomUtil.randomString(6) : app.getDeployKey();

        // 支持agent模式和传统模式的目录解析
        File outputDir = resolveOutputDir(appId, codeGenType);
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用未生成，请先生成代码！");
        }

        // Node项目需构建后部署dist产物，否则直接部署源码
        boolean nodeProject = isNodeProject(outputDir);
        File deploySource = nodeProject ? buildAndGetDist(outputDir) : outputDir;
        codeGenType=nodeProject?"default":codeGenType;
        String deployPath = AppConstant.DEPLOY_DIR + File.separator + codeGenType + "_" + deployKey;
        FileUtil.copyContent(deploySource, new File(deployPath), true);

        App update = new App();
        update.setId(appId);
        update.setDeployedTime(LocalDateTime.now());
        update.setDeployKey(deployKey);
        updateById(update);

        String webUrl = String.format("%s/%s_%s/", DEPLOY_HOST, codeGenType, deployKey);
        new Thread(() -> {
            String filePath = WebScreenShotUtil.getScreenShot(webUrl);
            String coverUrl = aliOSSManager.upload(filePath);
            update.setCover(coverUrl);
            updateById(update);
        }).start();

        return webUrl;
    }

    /**
     * 解析输出目录：优先agent_xxx，否则回退到传统路径
     */
    private File resolveOutputDir(Long appId, String codeGenType) {
        File agentDir = new File(AppConstant.OUTPUT_DIR, "agent_" + appId);
        if (agentDir.exists()) return agentDir;

        String legacyPath = codeGenType.equals("vue_project")
            ? "vue_project_" + appId
            : codeGenType + "_" + appId;
        return new File(AppConstant.OUTPUT_DIR, legacyPath);
    }

    /**
     * 检测是否为Node项目
     */
    private boolean isNodeProject(File dir) {
        return new File(dir, "package.json").exists();
    }

    /**
     * 构建Node项目并返回dist产物目录，失败重试3次
     */
    private File buildAndGetDist(File projectDir) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                runCommand(projectDir, packageManagerDetector.detect(projectDir.toPath()).getInstallCommand());
                runCommand(projectDir, packageManagerDetector.detect(projectDir.toPath()).getRunPrefix() + " build");

                File distDir = detectDistDir(projectDir);
                if (distDir != null) return distDir;

                if (attempt == 2) throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建产物目录未找到");
            } catch (Exception e) {
                log.error("构建失败 (attempt {}/3): {}", attempt, e.getMessage());
                if (attempt == 2) throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建失败: " + e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建失败");
    }

    /**
     * 检测构建产物目录：dist/build/out/.next
     */
    private File detectDistDir(File projectDir) {
        for (String name : new String[]{"dist", "build", "out"}) {
            File dir = new File(projectDir, name);
            if (dir.exists() && dir.isDirectory()) return dir;
        }
        return null;
    }

    /**
     * 执行命令，超时1200秒
     */
    private void runCommand(File workDir, String command) throws Exception {
        ProcessBuilder pb = System.getProperty("os.name").toLowerCase().contains("win")
            ? new ProcessBuilder("cmd", "/c", command)
            : new ProcessBuilder("sh", "-c", command);
        pb.directory(workDir).redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        if (!process.waitFor(1200, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new Exception("命令超时");
        }
        if (process.exitValue() != 0) throw new Exception("命令失败: " + output);
    }

    @Override
    public FileTreeNode getFileTree(Long appId) {
        App app = getById(appId);
        ThrowUtil.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        File projectDir = resolveProjectDir(appId, app.getCodeGenType());
        ThrowUtil.throwIf(!projectDir.exists(), ErrorCode.NOT_FOUND_ERROR, "项目目录不存在");

        return buildFileTree(projectDir, "");
    }

    @Override
    public String getFileContent(Long appId, String path) {
        ThrowUtil.throwIf(StrUtil.isBlank(path), ErrorCode.PARAMS_ERROR, "路径不能为空");
        ThrowUtil.throwIf(path.contains("..") || path.startsWith("/") || path.startsWith("\\"),
                ErrorCode.PARAMS_ERROR, "非法路径");

        App app = getById(appId);
        ThrowUtil.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        File projectDir = resolveProjectDir(appId, app.getCodeGenType());
        File file = new File(projectDir, path);

        try {
            String canonicalPath = file.getCanonicalPath();
            if (!canonicalPath.startsWith(projectDir.getCanonicalPath())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法路径访问");
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "路径解析失败");
        }

        ThrowUtil.throwIf(!file.exists() || !file.isFile(), ErrorCode.NOT_FOUND_ERROR, "文件不存在");

        return FileUtil.readUtf8String(file);
    }

    private File resolveProjectDir(Long appId, String codeGenType) {
        File agentDir = new File(AppConstant.OUTPUT_DIR + File.separator + "agent_" + appId);
        if (agentDir.exists()) {
            return agentDir;
        }
        return new File(AppConstant.OUTPUT_DIR + File.separator + codeGenType + "_" + appId);
    }

    private com.yzy.dto.FileTreeNode buildFileTree(File file, String basePath) {
        com.yzy.dto.FileTreeNode node = new com.yzy.dto.FileTreeNode();
        node.setName(file.getName().isEmpty() ? "root" : file.getName());

        if (file.isDirectory()) {
            node.setType("directory");
            List<com.yzy.dto.FileTreeNode> children = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    String childPath = basePath.isEmpty() ? child.getName() : basePath + "/" + child.getName();
                    children.add(buildFileTree(child, childPath));
                }
            }
            node.setChildren(children);
        } else {
            node.setType("file");
            node.setPath(basePath.isEmpty() ? file.getName() : basePath);
        }

        return node;
    }
}
