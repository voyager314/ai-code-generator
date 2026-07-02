package com.yzy.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzy.ai.context.CompressibleChatMemory;
import com.yzy.ai.context.TokenEstimator;
import com.yzy.dto.ChatHistoryAddRequest;
import com.yzy.dto.ChatHistoryQueryRequest;
import com.yzy.entity.App;
import com.yzy.entity.ChatHistory;
import com.yzy.enums.MessageTypeEnum;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.mapper.AppMapper;
import com.yzy.mapper.ChatHistoryMapper;
import com.yzy.service.ChatHistoryService;
import com.yzy.vo.ChatHistoryVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author yzy
 * @since 2026-03-14
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private AppMapper appMapper;

    @Override
    public Long saveChatHistory(ChatHistoryAddRequest request, Long userId) {
        // 验证消息类型
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getEnumByValue(request.getMessageType());
        if (messageTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息类型错误");
        }

        // 创建对话历史实体
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(request.getAppId())
                .userId(userId)
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .build();

        // 保存
        save(chatHistory);
        return chatHistory.getId();
    }

    @Override
    public Page<ChatHistoryVO> getChatHistoryByAppId(ChatHistoryQueryRequest request, Long userId) {
        ThrowUtil.throwIf(request.getAppId() == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        // 查询应用，验证权限
        App app = appMapper.selectOneById(request.getAppId());
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 验证用户权限（仅应用创建者可查看）
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看此应用的对话历史");
        }

        // 构建查询条件
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("appId", request.getAppId())
                .eq("isDelete", 0)
                .orderBy("createTime", false); // 按时间降序

        // 支持游标分页：加载比指定时间更早的记录
        if (request.getCreateTimeBefore() != null) {
            queryWrapper.lt("createTime", request.getCreateTimeBefore());
        }

        // 可选：过滤消息类型
        if (request.getMessageType() != null && !request.getMessageType().isEmpty()) {
            queryWrapper.eq("messageType", request.getMessageType());
        }

        // 默认每次加载10条，最多50条
        int pageSize = request.getPageSize();
        if (pageSize <= 0) {
            pageSize = 10;
        }
        pageSize = Math.min(pageSize, 50);
        int pageNum = request.getPageNum();

        // 执行分页查询
        Page<ChatHistory> page = page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        List<ChatHistoryVO> voList = getChatHistoryVOList(page.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Page<ChatHistoryVO> adminGetChatHistory(ChatHistoryQueryRequest request) {
        // 构建查询条件
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("isDelete", 0)
                .orderBy("createTime", false); // 按时间降序

        // 支持多条件查询
        if (request.getId() != null) {
            queryWrapper.eq("id", request.getId());
        }
        if (request.getAppId() != null) {
            queryWrapper.eq("appId", request.getAppId());
        }
        if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }
        if (request.getMessageType() != null && !request.getMessageType().isEmpty()) {
            queryWrapper.eq("messageType", request.getMessageType());
        }
        // 游标分页支持
        if (request.getCreateTimeBefore() != null) {
            queryWrapper.lt("createTime", request.getCreateTimeBefore());
        }

        // 管理员分页数量不限，但最多100条
        int pageSize = request.getPageSize();
        if (pageSize <= 0) {
            pageSize = 10;
        }
        pageSize = Math.min(pageSize, 100);
        int pageNum = request.getPageNum();

        // 执行分页查询
        Page<ChatHistory> page = page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        List<ChatHistoryVO> voList = getChatHistoryVOList(page.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Boolean deleteChatHistoryByAppId(Long appId) {
        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("appId", appId);
        return remove(queryWrapper);
    }

    @Override
    public Long saveChatHistory(Long appId, Long userId, String message, String messageType) {
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .build();
        save(chatHistory);
        return chatHistory.getId();
    }

    /**
     * 将ChatHistory列表转换为ChatHistoryVO列表
     */
    private List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (chatHistoryList == null || chatHistoryList.isEmpty()) {
            return List.of();
        }

        List<ChatHistoryVO> voList = new ArrayList<>(chatHistoryList.size());
        for (ChatHistory chatHistory : chatHistoryList) {
            ChatHistoryVO vo = new ChatHistoryVO();
            BeanUtil.copyProperties(chatHistory, vo);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 基于 token 预算从数据库加载对话历史到记忆中。
     * <p>
     * 策略：从 MySQL 按时间倒序分批查询（每批 50 条），逐条累加 token 估算值，
     * 达到预算上限或消息数硬上限（100 条）时停止，然后反转为时间升序写入记忆。
     * <p>
     * 对 CompressibleChatMemory 使用 replaceAll 一次性写入，
     * 避免逐条 add 造成 N 次 Redis 写入。
     */
    @Override
    public int loadChatHistory(Long appId, ChatMemory memory, int tokenBudget) {
        int batchSize = 50;
        // 跳过最新一条（正在处理的当前消息）
        int offset = 1;
        // 消息数硬上限，与 CompressibleChatMemory.MAX_STORED_MESSAGES 对齐
        int maxMessages = 100;
        List<ChatHistory> selected = new ArrayList<>();
        int accumulatedTokens = 0;
        boolean budgetExhausted = false;

        // 分批加载，逐条累加 token 直到预算用完
        while (!budgetExhausted && selected.size() < maxMessages) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(offset, batchSize);
            List<ChatHistory> batch = list(queryWrapper);
            if (batch == null || batch.isEmpty()) break;

            for (ChatHistory h : batch) {
                int msgTokens = TokenEstimator.estimateTokens(h.getMessage());
                if (accumulatedTokens + msgTokens > tokenBudget) {
                    budgetExhausted = true;
                    break;
                }
                selected.add(h);
                accumulatedTokens += msgTokens;
                if (selected.size() >= maxMessages) break;
            }

            offset += batchSize;
            // 本批不满说明没有更多数据
            if (batch.size() < batchSize) break;
        }

        if (selected.isEmpty()) return 0;

        // 还原为时间升序，保证对话时序正确
        Collections.reverse(selected);

        // 构建 LangChain4j 消息列表
        List<ChatMessage> messages = new ArrayList<>(selected.size());
        for (ChatHistory h : selected) {
            if (h.getMessageType().equals(MessageTypeEnum.USER.getValue())) {
                messages.add(UserMessage.from(h.getMessage()));
            } else {
                messages.add(AiMessage.from(h.getMessage()));
            }
        }

        // CompressibleChatMemory 走 replaceAll 一次性写入 Redis，其他类型逐条 add
        memory.clear();
        if (memory instanceof CompressibleChatMemory compressible) {
            compressible.replaceAll(messages);
        } else {
            for (ChatMessage msg : messages) {
                memory.add(msg);
            }
        }

        return selected.size();
    }
}

