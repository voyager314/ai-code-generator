package com.yzy.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRoutingServiceFactory {
    @Autowired
    private ChatModel chatModel;

    @Bean
    public AiRoutingService aiRoutingService(){
        return AiServices.builder(AiRoutingService.class).chatModel(chatModel).build();
    }
}
