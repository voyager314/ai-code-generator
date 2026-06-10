package com.yzy.controller;

import com.mybatisflex.core.paginate.Page;
import com.yzy.annotation.Auth;
import com.yzy.common.BaseResponse;
import com.yzy.common.ResultUtil;
import com.yzy.dto.ChatHistoryAddRequest;
import com.yzy.dto.ChatHistoryQueryRequest;
import com.yzy.entity.User;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.service.ChatHistoryService;
import com.yzy.vo.ChatHistoryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 对话历史 控制层。
 *
 * @author yzy
 * @since 2026-03-14
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 保存对话历史
     *
     * @param request 添加请求
     * @param requestObj HttpServletRequest
     * @return 对话历史id
     */
    @PostMapping("/save")
    public BaseResponse<Long> saveChatHistory(@RequestBody ChatHistoryAddRequest request,
                                               HttpServletRequest requestObj) {
        // 获取当前登录用户
        User loginUser = getLoginUser(requestObj);
        Long id = chatHistoryService.saveChatHistory(request, loginUser.getId());
        return ResultUtil.success(id);
    }

    /**
     * 查询应用对话历史（仅应用创建者和管理员可见）
     *
     * @param request 查询请求
     * @param requestObj HttpServletRequest
     * @return 分页结果
     */
    @GetMapping("/list/app")
    public BaseResponse<Page<ChatHistoryVO>> getChatHistoryByAppId(ChatHistoryQueryRequest request,
                                                                     HttpServletRequest requestObj) {
        User loginUser = getLoginUser(requestObj);
        Page<ChatHistoryVO> result = chatHistoryService.getChatHistoryByAppId(request, loginUser.getId());
        return ResultUtil.success(result);
    }

    // ========== 管理员接口 ==========

    /**
     * 管理员查询所有对话历史
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/admin/list")
    @Auth(role = "admin")
    public BaseResponse<Page<ChatHistoryVO>> adminGetChatHistory(ChatHistoryQueryRequest request) {
        Page<ChatHistoryVO> result = chatHistoryService.adminGetChatHistory(request);
        return ResultUtil.success(result);
    }

    /**
     * 获取当前登录用户
     */
    private User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("USER_LOGIN_STATE");
        ThrowUtil.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }
}
