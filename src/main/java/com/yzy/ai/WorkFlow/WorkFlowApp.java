package com.yzy.ai.WorkFlow;

import cn.hutool.json.JSONUtil;
import com.yzy.ai.WorkFlow.model.QualityResult;
import com.yzy.ai.WorkFlow.node.*;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.model.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
public class WorkFlowApp {

    /**
     * 执行工作流
     * @param originalPrompt 初始prompt
     * @param appId 应用id
     * @return
     */
    public Flux<String> executeWorkFlow(String originalPrompt, Long appId) {
        return Flux.create(sink->{
            new Thread(()->{
                // 创建工作流图
                CompiledGraph<MessagesState<String>> workflow = null;
                try {
                    workflow = new MessagesStateGraph<String>()
                            .addNode("image_collector", ImageCollect.create())
                            .addNode("prompt_enhancer", PromptEnhance.create())
                            .addNode("router", Router.create())
                            .addNode("code_generator", CodeGenerate.create())
                            .addNode("code_quality_check", CodeQualityCheckNode.create())
                            .addNode("project_builder", ProjectBuild.create())

                            // 添加边
                            .addEdge(START, "image_collector")// 开始 -> 图片收集
                            .addEdge("image_collector", "prompt_enhancer")// 图片收集 -> 提示词增强
                            .addEdge("prompt_enhancer", "router")// 提示词增强 -> 智能路由
                            .addEdge("router", "code_generator")// 智能路由 -> 代码生成
                            .addEdge("code_generator", "code_quality_check")

                            .addConditionalEdges("code_quality_check", AsyncEdgeAction.edge_async(this::passQualityCheck)
                                    , Map.of("build","project_builder","skip_build",END,"fail","code_generator"))

                            .addEdge("project_builder", END)
                            .compile();
                } catch (GraphStateException e) {
                    throw new RuntimeException(e);
                }

                WorkFlowContext initial = WorkFlowContext.builder()
                        .originalPrompt(originalPrompt)
                        .appId(appId)
                        .currentStep("初始化").build();
                log.info("初始输入{}", initial.getOriginalPrompt());

                log.info("开始执行工作流");
                sink.next(formatSseEvent("workflow start",
                        Map.of("msg","初始化工作流","originalPrompt",originalPrompt)));

                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("工作流图: \n{}", graph.content());

                // 执行工作流
                int stepCounter = 1;
                WorkFlowContext current = initial;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of(WorkFlowContext.WORKFLOW_CONTEXT_KEY,initial))) {
                    log.info("--- 第 {} 步完成 ---", stepCounter);
                    current=WorkFlowContext.getContext(step.state());
                    log.info("当前步骤上下文：{}",current);
                    sink.next(formatSseEvent("step completed",
                            Map.of("stepNumber",stepCounter,"currentStep",current.getCurrentStep())));
                    stepCounter++;
                }
                log.info("工作流执行完成！");
                sink.next(formatSseEvent("workflow completed",Map.of("msg","工作流执行完成")));
                sink.complete();
            }).start();
        });
    }

    /**
     * 格式化 SSE 事件的辅助方法
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }

    private String skipBuild(MessagesState<String> state){
        WorkFlowContext context = WorkFlowContext.getContext(state);
        CodeGenTypeEnum type = context.getGenerationType();
        if(!type.equals(CodeGenTypeEnum.VUE_PROJECT)){
            return "skip_build";
        }
        return "build";
    }

    private String passQualityCheck(MessagesState<String> state){
        WorkFlowContext context = WorkFlowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        if(!qualityResult.getIsValid()){
            log.warn("代码校验未通过");
            Integer retryCnt = context.getRetryCnt();
            if(retryCnt<=0){
                //限制校验失败的重试次数，避免无条件校验导致死循环
                log.warn("已达最大校验重试次数");
                return skipBuild(state);
            }
            context.setRetryCnt(retryCnt-1);
            return "fail";
        }
        //代码校验通过，使用原本的路由逻辑
        log.info("代码校验通过");
        return skipBuild(state);
    }
}
