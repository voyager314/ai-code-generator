package com.yzy.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yzy.ai.context.ContextCompressionProperties;
import com.yzy.ai.guardrail.PromptInputGuardRail;
import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.ai.tools.*;
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
public class AiCodeGenServiceFactory {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    @Qualifier("openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;//langchain4j根据yml配置自动生成的模型实例

    @Autowired
    @Qualifier("reasoningStreamingChatModel")
    private StreamingChatModel reasoningStreamingChatModel;//自定义的模型实例

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ToolManager toolManager;

    @Autowired
    private ContextCompressionProperties compressionProperties;

    //caffeine本地缓存每个应用的AiCodeGenService实例
    private final Cache<String,AiCodeGenService> aiCodeGenServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((k,v,cause)->{
                log.warn("AI服务实例被移除remove appId:{},cause:{}",k,cause);
            }).build();

    /**
     * 给每个应用创建AI服务实例
     * @param appId 应用id
     * @return AI服务实例
     */
    public AiCodeGenService createAiCodeGenService(long appId, CodeGenTypeEnum codeGenType) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(appId) //为每个对话记忆绑定一个appId实现对话隔离
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20).build();
        log.info("创建appId:{}的AiCodeGenService实例", appId);

        //创建服务实例时从数据库加载对话历史到redis缓存
        // 加载预算 = effectiveBudget × 60%，留 40% 给当前轮对话和 system prompt
        int loadBudget = (int) (compressionProperties.getEffectiveBudget() * 0.6);
        chatHistoryService.loadChatHistory(appId, memory, loadBudget);

        if(codeGenType.equals(CodeGenTypeEnum.VUE_PROJECT)){
            return AiServices.builder(AiCodeGenService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId->memory)
                    .tools(toolManager.getTools())
                    //配置幻觉工具名称策略
                    .hallucinatedToolNameStrategy(toolExecRequest-> ToolExecutionResultMessage.from(
                            toolExecRequest,"Error: There is no tool called "+toolExecRequest.name()
                    ))
                    //添加输入护轨
                    .inputGuardrails(new PromptInputGuardRail())
                    .build();
        }
        return AiServices.builder(AiCodeGenService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(memory)
                .inputGuardrails(new PromptInputGuardRail())
                .build();
    }

    public AiCodeGenService getAiCodeGenService(long appId, CodeGenTypeEnum codeGenType) {
        String key=appId+"_"+codeGenType.getValue();
        //如果和appId对应的服务实例存在就直接返回，否则先调用方法生成value再创建映射关系
        return aiCodeGenServiceCache.get(key,k->createAiCodeGenService(appId,codeGenType));
    }
}
