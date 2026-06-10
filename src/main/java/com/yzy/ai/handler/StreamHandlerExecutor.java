package com.yzy.ai.handler;

import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.entity.User;
import com.yzy.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class StreamHandlerExecutor {
    @Autowired
    private SimpleTextStreamHandler simpleTextStreamHandler;

    @Autowired
    private JsonStreamHandler jsonStreamHandler;

    public Flux<String> handle(Flux<String>origin,
                               Long appId, User loginUser,
                               ChatHistoryService service, CodeGenTypeEnum typeEnum){
        return switch (typeEnum){
            case VUE_PROJECT -> jsonStreamHandler.handle(origin,appId,loginUser,service);
            case HTML,MULTI_FILE -> simpleTextStreamHandler.handle(origin,appId,loginUser,service);
            default -> Flux.empty();
        };
    }
}
