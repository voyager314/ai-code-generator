package com.yzy.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.yzy.dto.ChatHistoryAddRequest;
import com.yzy.dto.ChatHistoryQueryRequest;
import com.yzy.entity.ChatHistory;
import com.yzy.vo.ChatHistoryVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 对话历史 服务层。
 *
 * @author yzy
 * @since 2026-03-14
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存对话历史
     *
     * @param request 添加请求
     * @param userId  用户id
     * @return 对话历史id
     */
    Long saveChatHistory(ChatHistoryAddRequest request, Long userId);

    /**
     * 根据应用id查询对话历史（应用创建者可见）
     *
     * @param request 查询请求
     * @param userId  当前用户id
     * @return 分页结果
     */
    Page<ChatHistoryVO> getChatHistoryByAppId(ChatHistoryQueryRequest request, Long userId);

    /**
     * 管理员查询所有对话历史
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<ChatHistoryVO> adminGetChatHistory(ChatHistoryQueryRequest request);

    /**
     * 删除应用的所有对话历史
     *
     * @param appId 应用id
     * @return 是否成功
     */
    Boolean deleteChatHistoryByAppId(Long appId);

    /**
     * 根据应用id和消息类型保存对话历史
     *
     * @param appId       应用id
     * @param userId      用户id
     * @param message     消息内容
     * @param messageType 消息类型
     * @return 对话历史id
     */
    Long saveChatHistory(Long appId, Long userId, String message, String messageType);

    /**
     * 加载数据库对话历史到记忆中
     * @param appId 应用id
     * @param memory 对话记忆
     * @param maxCnt 每次最大可加载条数
     * @return 历史对话条数
     */
    int loadChatHistory(Long appId, MessageWindowChatMemory memory,int maxCnt);

}
