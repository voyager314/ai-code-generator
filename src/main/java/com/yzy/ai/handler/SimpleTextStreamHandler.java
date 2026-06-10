package com.yzy.ai.handler;

import com.yzy.entity.User;
import com.yzy.enums.MessageTypeEnum;
import com.yzy.service.ChatHistoryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SimpleTextStreamHandler {
    /**
     * 直接收集完整的文本响应
     * @param origin 原始响应流
     * @param appId 应用id
     * @param loginUser 已登录用户
     * @param service 对话历史服务
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> origin, Long appId, User loginUser, ChatHistoryService service) {
        StringBuilder sb=new StringBuilder();
        return origin
                .map(chunk->{
                    //收集AI响应内容
                    sb.append(chunk);
                    return chunk;
                })
                .doOnComplete(()->{
                    //回复完后添加消息到数据库
                    service.saveChatHistory(appId,loginUser.getId(),sb.toString(),MessageTypeEnum.AI.getValue());
                })
                .doOnError(throwable->{
                    //发生错误也要记录
                    service.saveChatHistory(appId,loginUser.getId(),throwable.getMessage(),MessageTypeEnum.AI.getValue());
                });
    }
}
