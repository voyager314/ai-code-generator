package com.yzy.ai;

import com.yzy.ai.WorkFlow.tools.ImageSearchTool;
import com.yzy.ai.WorkFlow.tools.LogoGenerateTool;
import com.yzy.ai.WorkFlow.tools.MermaidTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageCollectionServiceFactory {
    @Autowired
    private ImageSearchTool imageSearchTool;
    @Autowired
    private LogoGenerateTool logoGenerateTool;
    @Autowired
    private MermaidTool mermaidTool;

    @Autowired
    private ChatModel chatModel;

    @Bean
    public ImageCollectionService imageCollectionService(){
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(imageSearchTool,logoGenerateTool,mermaidTool)
                .build();
    }

}
