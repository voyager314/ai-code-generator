package com.yzy.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yzy.ai.guardrail.PromptInputGuardRail;
import com.yzy.ai.tools.ToolManager;
import com.yzy.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

    private final Cache<Long, CodingAgentService> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((k, v, cause) ->
                    log.warn("CodingAgentService 实例被移除 appId:{}, cause:{}", k, cause))
            .build();

    public CodingAgentService getService(Long appId) {
        return cache.get(appId, this::createService);
    }

    private CodingAgentService createService(Long appId) {
        log.info("创建 appId:{} 的 CodingAgentService 实例", appId);

        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(30)
                .build();

        chatHistoryService.loadChatHistory(appId, memory, 30);

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
}
