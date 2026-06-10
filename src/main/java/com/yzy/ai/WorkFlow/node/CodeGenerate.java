package com.yzy.ai.WorkFlow.node;

import com.yzy.ai.AiCodeGeneratorFacade;
import com.yzy.ai.WorkFlow.model.QualityResult;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.WorkFlow.util.SpringContextUtil;
import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.common.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Slf4j
public class CodeGenerate {
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            AiCodeGeneratorFacade bean = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            CodeGenTypeEnum type = context.getGenerationType();

            Long appId = context.getAppId();

            String usermsg = context.getEnhancedPrompt();
            QualityResult qualityResult = context.getQualityResult();
            if(qualityResult!=null&&!qualityResult.getIsValid()
                    &&qualityResult.getErrors()!=null&&!qualityResult.getErrors().isEmpty()) {
                usermsg=buildErrorFixPrompt(qualityResult);
            }
            Flux<String> flux = bean.genAndSaveCode(usermsg, type, appId);
            flux.blockLast(Duration.ofMinutes(30));
            String codePath= AppConstant.OUTPUT_DIR+ File.separator+String.format("%s_%s",type,appId);
            context.setGeneratedCodeDir(codePath);
            context.setCurrentStep("生成代码");
            log.info("代码生成完毕");
            return WorkFlowContext.saveContext(context);
        });
    }

    private static String buildErrorFixPrompt(QualityResult qualityResult){
        StringBuilder errorFixPrompt = new StringBuilder();
        errorFixPrompt.append("\n\n## 上次生成的代码存在如下问题\n");
        qualityResult.getErrors().forEach(error->{errorFixPrompt.append("- ").append(error).append("\n");});
        List<String> suggestions = qualityResult.getSuggestions();
        if(suggestions!=null && !suggestions.isEmpty()){
            errorFixPrompt.append("\n\n## 修改建议如下\n");
            suggestions.forEach(t->errorFixPrompt.append("- ").append(t).append("\n"));
        }
        return errorFixPrompt.toString();
    }
}
