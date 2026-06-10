package com.yzy.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeQualityCheckFactory {
    @Autowired
    private ChatModel chatModel;

    @Bean
    public CodeQualityCheckService codeQualityCheckService(){
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel).build();
    }
}
