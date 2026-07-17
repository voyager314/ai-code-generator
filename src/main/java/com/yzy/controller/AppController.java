package com.yzy.controller;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.yzy.ai.AiCodeGeneratorFacade;
import com.yzy.ai.approval.ApprovalService;
import com.yzy.annotation.Auth;
import com.yzy.annotation.RateLimit;
import com.yzy.common.AppConstant;
import com.yzy.common.BaseResponse;
import com.yzy.common.ResultUtil;
import com.yzy.dto.*;
import com.yzy.entity.App;
import com.yzy.entity.User;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.manager.AliOSSManager;
import com.yzy.service.AppService;
import com.yzy.service.ChatHistoryService;
import com.yzy.service.ProjectDownLoadService;
import com.yzy.service.UserService;
import com.yzy.vo.AppDetailVO;
import com.yzy.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * 应用 控制层。
 *
 * @author yzy
 * @since 2026-03-12
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ProjectDownLoadService projectDownLoadService;

    @Autowired
    private AliOSSManager uoloadManager;

    /**
     * 创建应用
     *
     * @param request 创建应用请求
     * @return 应用id
     */
    @PostMapping("/create")
    @Auth(role = "user")
    public BaseResponse<Long> createApp(@RequestBody AppAddRequest request) {
        Long appId = appService.createApp(request);
        return ResultUtil.success(appId);
    }

    /**
     * 更新应用
     *
     * @param request 更新应用请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @Auth(role = "user")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest request) {
        Boolean result = appService.updateApp(request.getId(), request);
        return ResultUtil.success(result);
    }

    /**
     * 删除应用
     *
     * @param request 删除应用请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Auth(role = "user")
    public BaseResponse<Boolean> deleteApp(@RequestBody AppDeleteRequest request) {
        Boolean result = appService.deleteApp(request.getId());
        return ResultUtil.success(result);
    }

    /**
     * 获取应用详情
     *
     * @param id 应用id
     * @return 应用详情
     */
    @GetMapping("/get/{id}")
    public BaseResponse<AppDetailVO> getAppDetail(@PathVariable Long id) {
        AppDetailVO detail = appService.getAppDetail(id);
        return ResultUtil.success(detail);
    }

    /**
     * 分页查询自己的应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/list/page")
    @Auth(role = "user")
    public BaseResponse<Page<AppVO>> getMyAppList(AppQueryRequest request) {
        Page<AppVO> result = appService.getMyAppList(request);
        return ResultUtil.success(result);
    }

    /**
     * 分页查询精选的应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/star/page")
    public BaseResponse<Page<AppVO>> getStarAppList(AppStarQueryRequest request) {
        Page<AppVO> result = appService.getStarAppList(request);
        return ResultUtil.success(result);
    }

    // ========== 管理员接口 ==========

    /**
     * 管理员更新应用
     *
     * @param request 更新应用请求
     * @return 是否成功
     */
    @PostMapping("/admin/update")
    @Auth(role = "admin")
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppUpdateRequest request) {
        Boolean result = appService.adminUpdateApp(request.getId(), request);
        return ResultUtil.success(result);
    }

    /**
     * 管理员删除应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    @PostMapping("/admin/delete/{id}")
    @Auth(role = "admin")
    public BaseResponse<Boolean> adminDeleteApp(@PathVariable Long id) {
        Boolean result = appService.adminDeleteApp(id);
        return ResultUtil.success(result);
    }

    /**
     * 管理员分页查询应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/admin/list")
    @Auth(role = "admin")
    public BaseResponse<Page<AppVO>> adminGetAppList(AppQueryRequest request) {
        Page<AppVO> result = appService.adminGetAppList(request);
        return ResultUtil.success(result);
    }

    /**
     * 管理员获取应用详情
     *
     * @param id 应用id
     * @return 应用详情
     */
    @GetMapping("/admin/get/{id}")
    @Auth(role = "admin")
    public BaseResponse<AppDetailVO> adminGetAppDetail(@PathVariable Long id) {
        AppDetailVO detail = appService.getAppDetail(id);
        return ResultUtil.success(detail);
    }

    /**
     * 应用聊天生成代码（SSE流式）
     * @param appId 应用id
     * @param msg 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(key="rate_limit",message = "AI对话请求过于频繁，请稍后重试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId, @RequestParam String msg,
                                                       HttpServletRequest request,
                                                       @RequestParam(value = "file",required = false) MultipartFile[] files,
                                                       @RequestParam(defaultValue = "true") boolean agent) {
        Object user = request.getSession().getAttribute("USER_LOGIN_STATE");
        ThrowUtil.throwIf(user==null, ErrorCode.NOT_LOGIN_ERROR);
        User loginUser = (User) user;

        // 保存用户消息到对话历史
        chatHistoryService.saveChatHistory(appId, loginUser.getId(), msg, "user");
        if(files != null && files.length > 0) {
            try {
                StringBuilder sb=new StringBuilder();
                sb.append(msg).append("\n\n参考文件: \n");
                String path=AppConstant.FILE_DIR+File.separator+loginUser.getId()+File.separator+appId;
                File fileDir = new File(path);
                if(!fileDir.exists()) {fileDir.mkdirs();}
                for(MultipartFile file : files) {
                    String originalFilename = file.getOriginalFilename();
                    String newFilename = UUID.randomUUID().toString()+originalFilename.substring(originalFilename.lastIndexOf("."));
                    File file1 = new File(fileDir, newFilename);
                    file.transferTo(file1);
                    String url = uoloadManager.upload(file1.getAbsolutePath());
                    sb.append(url).append("\n");
                }
                msg=sb.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Flux<String> stringFlux = appService.chatToGenCode(appId, msg, loginUser,agent);

        // 用于收集 AI 回复内容
        StringBuilder aiResponse = new StringBuilder();

        return stringFlux
                .doOnNext(chunk -> aiResponse.append(chunk))
                .map(chunk->{
                    Map<String,String> map=Map.of("d",chunk);
                    //将内容包装成json对象
                    String jsonStr = JSONUtil.toJsonStr(map);
                    return ServerSentEvent.<String>builder().data(jsonStr).build();
                })
                // 保存 AI 回复到对话历史
                .doOnComplete(() -> {
                    if (aiResponse.length() > 0) {
                        chatHistoryService.saveChatHistory(appId, loginUser.getId(), aiResponse.toString(), "ai");
                    }
                })
                // 如果发生错误，保存错误信息
                .doOnError(throwable -> {
                    String errorMsg = throwable.getMessage();
                    if (errorMsg == null) {
                        errorMsg = "AI 回复失败";
                    }
                    chatHistoryService.saveChatHistory(appId, loginUser.getId(), errorMsg, "ai");
                })
                //在后方追加结束事件，便于前端判断是否异常终止
                .concatWith(Mono.just(ServerSentEvent.<String>builder().event("done").data("").build()));
    }

    /**
     * Agent 审批回调
     * @param request 审批请求
     * @return 是否成功
     */
    @PostMapping("/agent/approve")
    public BaseResponse<Boolean> approveAgentAction(@RequestBody AgentApprovalRequest request) {
        boolean result = approvalService.submitApproval(request.getApprovalId(), request.isApproved(), request.getCustomMessage());
        return ResultUtil.success(result);
    }

    /**
     * 部署应用
     * @param deployRequest 部署请求
     * @param request 请求实体
     * @return 应用可访问的url
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest deployRequest,HttpServletRequest request){
        Object user = request.getSession().getAttribute("USER_LOGIN_STATE");
        ThrowUtil.throwIf(user==null, ErrorCode.NOT_LOGIN_ERROR);
        Long appId = deployRequest.getAppId();
        ThrowUtil.throwIf(appId==null, ErrorCode.PARAMS_ERROR,"appId不能为空！");
        String url = appService.deployApp(appId, (User) user);
        return ResultUtil.success(url);
    }

    /**
     * 下载项目文件
     * @param appId 应用id
     * @param request 请求对象
     * @param response 响应对象
     */
    @GetMapping("/downLoad")
    public void downLoadProject(@RequestParam Long appId, HttpServletRequest request , HttpServletResponse response) {
        App app = appService.getById(appId);
        if(app==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"应用不存在！");
        }
        Object state = request.getSession().getAttribute("USER_LOGIN_STATE");
        if(state==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录！");
        }
        User user=(User)state;
        if(!user.getId().equals(app.getUserId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无应用下载权限！");
        }

        // 优先检查agent目录，否则使用传统路径
        String projectPath;
        File agentDir = new File(AppConstant.OUTPUT_DIR + File.separator + "agent_" + appId);
        if (agentDir.exists()) {
            projectPath = agentDir.getAbsolutePath();
        } else {
            projectPath = AppConstant.OUTPUT_DIR + File.separator + app.getCodeGenType() + "_" + appId;
        }

        projectDownLoadService.downLoadAsZip(projectPath,String.valueOf(appId),response);
    }

    /**
     * 获取应用文件树
     * @param appId 应用id
     * @return 文件树
     */
    @GetMapping("/files/{appId}")
    public BaseResponse<FileTreeNode> getAppFileTree(@PathVariable Long appId) {
        FileTreeNode tree = appService.getFileTree(appId);
        return ResultUtil.success(tree);
    }

    /**
     * 获取应用文件内容
     * @param appId 应用id
     * @param path 文件相对路径
     * @return 文件内容
     */
    @GetMapping("/file/{appId}")
    public BaseResponse<String> getAppFileContent(@PathVariable Long appId, @RequestParam String path) {
        String content = appService.getFileContent(appId, path);
        return ResultUtil.success(content);
    }
}
