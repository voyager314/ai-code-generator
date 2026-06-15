package com.yzy.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yzy.ai.context.CompressibleChatMemory;
import com.yzy.ai.context.ContextCompressionProperties;
import com.yzy.ai.context.TokenTracker;
import com.yzy.ai.context.ToolOutputArchiver;
import com.yzy.ai.guardrail.PromptInputGuardRail;
import com.yzy.ai.tools.ToolManager;
import com.yzy.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CodingAgentService 工厂
 * <p>
 * 为每个 appId 创建并缓存独立的 Agent 实例（Caffeine 本地缓存，30 分钟过期）。
 * 每个实例绑定：
 * - reasoningStreamingChatModel（支持 tool calling 的流式模型）
 * - Redis-backed ChatMemory（30 条消息窗口，会话隔离）
 * - 全部工具（通过 ToolManager 自动注入）
 * - 输入护栏和幻觉工具名处理
 */
@Slf4j
@Component
public class CodingAgentServiceFactory {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    @Qualifier("reasoningStreamingChatModel")
    private StreamingChatModel reasoningStreamingChatModel;

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ToolManager toolManager;

    @Autowired
    private ContextCompressionProperties compressionProperties;

    @Autowired
    private TokenTracker tokenTracker;

    @Autowired
    private ToolOutputArchiver toolOutputArchiver;

    private final ConcurrentHashMap<Long, CompressibleChatMemory> memoryRegistry = new ConcurrentHashMap<>();

    private final Cache<Long, CodingAgentService> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((k, v, cause) -> {
                log.warn("CodingAgentService 实例被移除 appId:{}, cause:{}", k, cause);
                memoryRegistry.remove(k);
            })
            .build();

    /**
     * 获取或创建 Agent 服务实例（缓存命中则复用）
     */
    public CodingAgentService getService(Long appId) {
        return cache.get(appId, this::createService);
    }

    private CodingAgentService createService(Long appId) {
        log.info("创建 appId:{} 的 CodingAgentService 实例", appId);

        CompressibleChatMemory memory = new CompressibleChatMemory(
                appId, redisChatMemoryStore, compressionProperties, tokenTracker, toolOutputArchiver);

        chatHistoryService.loadChatHistory(appId, memory, 30);
        memoryRegistry.put(appId, memory);

        return AiServices.builder(CodingAgentService.class)
                .chatModel(chatModel)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> memory)
                .tools(toolManager.getTools())
                .hallucinatedToolNameStrategy(toolExecRequest ->
                        ToolExecutionResultMessage.from(toolExecRequest,
                                "Error: There is no tool called " + toolExecRequest.name()))
                .inputGuardrails(new PromptInputGuardRail())
                .build();
    }

    /**
     * 扫描并修复记忆中所有非法的工具调用消息序列。
     * <p>
     * 处理的非法模式：
     * - AiMessage(tool_calls) 缺少部分或全部 ToolExecutionResultMessage
     * - 孤立的 ToolExecutionResultMessage（对应的 AiMessage 不存在）
     */
    public void sanitizeMemory(Long appId) {
        CompressibleChatMemory memory = memoryRegistry.get(appId);
        if (memory == null) return;

        List<ChatMessage> raw = memory.rawMessages();
        if (raw.isEmpty()) return;

        List<ChatMessage> sanitized = sanitizeMessages(raw);
        int removed = raw.size() - sanitized.size();
        if (removed == 0) return;

        log.warn("sanitizeMemory: appId={} 移除了 {} 条非法消息", appId, removed);
        memory.replaceAll(sanitized);
    }

    /**
     * 线性扫描消息列表，丢弃不完整的工具调用组和孤立的工具结果。
     * <p>
     * 合法的工具调用组：AiMessage(tool_calls=[id1,id2]) 紧跟 ToolExecutionResultMessage(id1), ToolExecutionResultMessage(id2)。
     * 组内 tool result 数量与 tool call 数量不一致时，整组丢弃。
     */
    private List<ChatMessage> sanitizeMessages(List<ChatMessage> messages) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
        int i = 0;

        while (i < messages.size()) {
            ChatMessage msg = messages.get(i);

            if (msg instanceof AiMessage ai && ai.hasToolExecutionRequests()) {
                Set<String> expectedIds = ai.toolExecutionRequests().stream()
                        .map(r -> r.id())
                        .collect(Collectors.toSet());

                List<ToolExecutionResultMessage> collected = new ArrayList<>();
                int j = i + 1;
                while (j < messages.size() && messages.get(j) instanceof ToolExecutionResultMessage tres) {
                    if (expectedIds.contains(tres.id())) {
                        collected.add(tres);
                    }
                    j++;
                }

                if (collected.size() == expectedIds.size()) {
                    result.add(ai);
                    result.addAll(collected);
                } else {
                    log.warn("sanitizeMessages: 丢弃不完整的工具调用组, expected={}, got={}",
                            expectedIds.size(), collected.size());
                }
                i = j;
            } else if (msg instanceof ToolExecutionResultMessage tres) {
                log.warn("sanitizeMessages: 丢弃孤立的 ToolExecutionResultMessage, id={}", tres.id());
                i++;
            } else {
                result.add(msg);
                i++;
            }
        }
        return result;
    }
}
