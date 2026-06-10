package com.yzy.ai.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yzy.ai.builder.VueProjectBuilder;
import com.yzy.ai.model.*;
import com.yzy.common.AppConstant;
import com.yzy.entity.User;
import com.yzy.enums.MessageTypeEnum;
import com.yzy.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class JsonStreamHandler {
    @Autowired
    private VueProjectBuilder vueProjectBuilder;
    /**
     * 处理vue项目的json流
     * @param origin 原始流
     * @param appId 应用id
     * @param loginUser 已登录用户
     * @param service 对话历史服务
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String>origin, Long appId, User loginUser, ChatHistoryService service){
        StringBuilder sb=new StringBuilder();
        //存储每个用过的工具
        Set<String> set=new HashSet<>();
        return origin
                .map(chunk->{
                    return handleJsonMessageChunk(chunk,sb,set);
                })
                .filter(StrUtil::isNotBlank)
                .doOnComplete(()->{
                    service.saveChatHistory(appId,loginUser.getId(),sb.toString(), MessageTypeEnum.AI.getValue());
                    new Thread(()->{
                        //在json流处理完毕后开启异步线程构建vue roject
                        //为了保证兼容性，这里暂时不使用java21虚拟线程
                        String projectPath=AppConstant.OUTPUT_DIR+ File.separator+"vue_project_"+appId;
                        boolean b = vueProjectBuilder.buildVueProject(projectPath);
                        if(!b){
                            log.error("vue project build failed!");
                        }
                    }).start();
                })
                .doOnError(throwable->{
                    service.saveChatHistory(appId,loginUser.getId(),throwable.getMessage(), MessageTypeEnum.AI.getValue());
                });
    }

    /**
     * 收集并解析TokenStream
     * @param chunk 数据块
     * @param sb 数据集
     * @param seenToolIds 已使用的tool的id set
     * @return 解析的TokenStream结果
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder sb, Set<String>seenToolIds){
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum anEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (anEnum){
            case AI_RESPONSE -> {
                AiResponseMessage message = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = message.getData();
                sb.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage message = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = message.getId();
                if(toolId!=null&&!seenToolIds.contains(toolId)){
                    //如果是首次调用工具，记录id并返回完整信息
                    seenToolIds.add(toolId);
                    return "\n\n[选择工具]写入文件\n\n";
                }
                return "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage message = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(message.getArguments());
                String relativePath = jsonObject.getStr("relativePath");
                String suffix = FileUtil.getSuffix(relativePath);
                String content = jsonObject.getStr("content");
                String result=String.format("""
                        [工具调用]写入文件 %s
                        ```%s
                        %s
                        ```
                        """,relativePath,content,suffix);
                //输出前端要持久化的内容
                String output=String.format("\n\n%s\n\n",result);
                sb.append(output);
                return output;
            }
            default -> {
                log.error("不支持该消息类型:{}",anEnum);
                return "";
            }
        }

    }
}
