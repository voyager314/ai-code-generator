package com.yzy.ai;

import cn.hutool.json.JSONUtil;
import com.yzy.ai.model.*;
import com.yzy.ai.parser.CodeParserExecuter;
import com.yzy.ai.saver.CodeFileSaverExecuter;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * ai代码生成门面类，组合代码生成和保存
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {
    @Autowired
    private AiCodeGenServiceFactory serviceFactory;

    /**
     * 统一入口，根据代码生成类型生成并保存代码
     * @param userMsg 用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId 应用id
     * @return 代码路径
     */
    public Flux<String> genAndSaveCode(String userMsg, CodeGenTypeEnum codeGenTypeEnum,Long appId){
        log.info("genAndSaveCode called with type: {}, message: {}, appId: {}", codeGenTypeEnum, userMsg, appId);
        try {
            return switch (codeGenTypeEnum){
                case HTML->{
                    log.info("Routing to HTML stream method");
                    yield genAndSaveHtmlCodeStream(userMsg,appId);
                }
                case MULTI_FILE->{
                    log.info("Routing to MULTI_FILE stream method");
                    yield genAndSaveMultiFileCodeStream(userMsg,appId);
                }
                case VUE_PROJECT -> {
                    log.info("Routing to VUE_PROJECT stream method");
                    TokenStream tokenStream = serviceFactory
                            .getAiCodeGenService(appId, codeGenTypeEnum)
                            .genVueProjectTokenStream(appId, userMsg);
                    yield processTokenStream(tokenStream);
                }
                default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"不支持的生成类型！");
            };
        } catch (Exception e) {
            log.error("genAndSaveCode error", e);
            return Flux.error(e);
        }
    }

    /**
     * 流式获取html代码输出
     * @param userMsg
     * @param appId 应用id
     * @return
     */
    public Flux<String> genAndSaveHtmlCodeStream(String userMsg,Long appId){
        log.info("genAndSaveHtmlCodeStream called with message: {}, appId: {}", userMsg, appId);
        Flux<String> stringFlux;
        try {
            stringFlux = serviceFactory.getAiCodeGenService(appId,CodeGenTypeEnum.HTML).genHtmlCodeStream(userMsg);
        } catch (Exception e) {
            log.error("genAndSaveHtmlCodeStream error", e);
            return Flux.error(e);
        }
        //使用StringBuilder实时保存流式输出
        StringBuilder sb=new StringBuilder();
        return stringFlux
                .doOnSubscribe(s -> log.info("HTML stream subscribed"))
                .doOnNext(s -> {
                    sb.append(s);
                    log.info("Received chunk: {}", s.length());
                })
                .doOnComplete(()->{
                    String code=sb.toString();
                    log.info("HTML stream completed, total length: {}", code.length());
                    //所有输出收集完成后提取代码并保存
                    HtmlCodeResult htmlCodeResult =(HtmlCodeResult) CodeParserExecuter.executeCodeParse(code,CodeGenTypeEnum.HTML);
                    File file = CodeFileSaverExecuter.executeCodeSave(CodeGenTypeEnum.HTML,htmlCodeResult,appId);
                    log.info("文件保存成功{}",file);
                })
                .doOnError(e -> log.error("HTML stream error", e));
    }

    /**
     * 流式获取多文件代码输出
     * @param userMsg
     * @param appId 应用id
     * @return
     */
    public Flux<String> genAndSaveMultiFileCodeStream(String userMsg,Long appId){
        log.info("genAndSaveMultiFileCodeStream called with message: {}, appId: {}", userMsg, appId);
        Flux<String> stringFlux;
        try {
            stringFlux = serviceFactory.getAiCodeGenService(appId,CodeGenTypeEnum.MULTI_FILE).genMultiFileCodeStream(userMsg);
        } catch (Exception e) {
            log.error("genAndSaveMultiFileCodeStream error", e);
            return Flux.error(e);
        }
        StringBuilder sb=new StringBuilder();
        return stringFlux
                .doOnSubscribe(s -> log.info("Multi-file stream subscribed"))
                .doOnNext(s -> {
                    sb.append(s);
                    log.info("Received chunk: {}", s.length());
                })
                .doOnComplete(()->{
                    String code=sb.toString();
                    log.info("Multi-file stream completed, total length: {}", code.length());
                    MultiFileCodeResult multiFileCodeResult =(MultiFileCodeResult) CodeParserExecuter.executeCodeParse(code,CodeGenTypeEnum.MULTI_FILE);
                    File file = CodeFileSaverExecuter.executeCodeSave(CodeGenTypeEnum.MULTI_FILE,multiFileCodeResult,appId);
                    log.info("文件保存成功{}",file);
                })
                .doOnError(e -> log.error("Multi-file stream error", e));
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .beforeToolExecution((BeforeToolExecution beforeToolExecution) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(beforeToolExecution.request());
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        log.error("processTokenStream error", error);
                        sink.error(error);
                    })
                    .start();
        });
    }

}
