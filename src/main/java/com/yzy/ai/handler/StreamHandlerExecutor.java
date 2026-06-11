package com.yzy.ai.handler;

import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.entity.User;
import com.yzy.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流式处理路由器
 * <p>
 * 根据模式将原始 Flux 流路由到不同的 Handler：
 * - handle()：非 Agent 模式，按 CodeGenTypeEnum 分发到 JsonStreamHandler 或 SimpleTextStreamHandler
 * - handleAgent()：Agent 模式，统一路由到 AgentStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {
    @Autowired
    private SimpleTextStreamHandler simpleTextStreamHandler;

    @Autowired
    private JsonStreamHandler jsonStreamHandler;

    @Autowired
    private AgentStreamHandler agentStreamHandler;

    public Flux<String> handle(Flux<String>origin,
                               Long appId, User loginUser,
                               ChatHistoryService service, CodeGenTypeEnum typeEnum){
        return switch (typeEnum){
            case VUE_PROJECT -> jsonStreamHandler.handle(origin,appId,loginUser,service);
            case HTML,MULTI_FILE -> simpleTextStreamHandler.handle(origin,appId,loginUser,service);
            case DEFAULT -> simpleTextStreamHandler.handle(origin,appId,loginUser,service);
            default -> Flux.empty();
        };
    }

    public Flux<String> handleAgent(Flux<String> origin,
                                    Long appId, User loginUser,
                                    ChatHistoryService service) {
        return agentStreamHandler.handle(origin, appId, loginUser, service);
    }
}
