package com.yzy.ai.WorkFlow.node;

import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.WorkFlow.util.SpringContextUtil;
import com.yzy.ai.builder.VueProjectBuilder;
import com.yzy.common.AppConstant;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

@Slf4j
public class ProjectBuild {
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            String codeDir = context.getGeneratedCodeDir();
            VueProjectBuilder builder = SpringContextUtil.getBean(VueProjectBuilder.class);
            boolean b = builder.buildVueProject(codeDir);
            if(b){
                context.setBuildResultDir(AppConstant.OUTPUT_DIR+ File.separator+"dist");
            }else throw new BusinessException(ErrorCode.SYSTEM_ERROR,"vue项目构建失败");
            context.setCurrentStep("构建项目");
            log.info("项目构建成功");
            return WorkFlowContext.saveContext(context);
        });
    }
}
