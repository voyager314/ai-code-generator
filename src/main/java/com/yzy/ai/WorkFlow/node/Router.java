package com.yzy.ai.WorkFlow.node;

import com.yzy.ai.AiRoutingService;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.WorkFlow.util.SpringContextUtil;
import com.yzy.ai.model.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

@Slf4j
public class Router {
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            AiRoutingService service = SpringContextUtil.getBean(AiRoutingService.class);
            //根据初始提示词确定代码生成类型
            CodeGenTypeEnum codeGenTypeEnum = service.aiRoutingService(context.getOriginalPrompt());
            context.setGenerationType(codeGenTypeEnum);
            context.setCurrentStep("智能路由");
            log.info("路由至{}",context.getGenerationType());
            return WorkFlowContext.saveContext(context);
        });
    }
}
