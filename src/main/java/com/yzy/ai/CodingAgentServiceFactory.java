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
     * 移除记忆末尾的孤立工具调用消息（有 tool_calls 但缺少对应 tool result 的 AiMessage）。
     * 当工具参数解析失败导致 AiMessage 已写入 Redis 但 ToolExecutionResultMessage 未写入时调用，
     * 防止下一轮 API 请求因非法消息序列被 DeepSeek 拒绝。
     */
    public void sanitizeMemory(Long appId) {
        CompressibleChatMemory memory = memoryRegistry.get(appId);
        if (memory == null) return;

        List<ChatMessage> messages = memory.rawMessages();
        if (messages.isEmpty()) return;

        int cutTo = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (!(msg instanceof AiMessage aiMsg) || !aiMsg.hasToolExecutionRequests()) break;

            Set<String> callIds = aiMsg.toolExecutionRequests().stream()
                    .map(r -> r.id())
                    .collect(Collectors.toSet());

            for (int j = i + 1; j < messages.size(); j++) {
                if (messages.get(j) instanceof ToolExecutionResultMessage res) {
                    callIds.remove(res.id());
                }
            }

            if (!callIds.isEmpty()) {
                cutTo = i;
            }
            break;
        }

        if (cutTo >= 0) {
            log.warn("sanitizeMemory: 移除 appId={} 记忆中 {} 条孤立工具调用消息", appId,
                    messages.size() - cutTo);
            List<ChatMessage> sanitized = new ArrayList<>(messages.subList(0, cutTo));
            memory.clear();
            sanitized.forEach(memory::add);
        }
    }
}
