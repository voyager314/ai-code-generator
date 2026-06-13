package com.yzy.config;

import com.yzy.listener.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {
    @Autowired
    private AiModelMonitorListener aiModelMonitorListener;

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private Duration timeout;

    /**
     * 推理流式模型
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        // 生产环境使用：
        // final String modelName = "deepseek-reasoner";
        // final int maxTokens = 32768;
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(true)
                .logResponses(true)
                //注册监听器
                .listeners(aiModelMonitorListener)
                .build();
    }
}

