package com.yzy.ai.WorkFlow.node;

import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

@Slf4j
public class PromptEnhance {
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            List<ImageResource> imageList = context.getImageList();
            StringBuilder enhancedPrompt = new StringBuilder();
            if(imageList!=null && !imageList.isEmpty()){
                enhancedPrompt.append("\n\n## 可用图片资源\n");
                enhancedPrompt.append(("请在生成应用时使用以下图片资源，合理的把图片嵌入到页面相应位置。\n"));
                for(ImageResource image : imageList){
                    enhancedPrompt.append("- ");
                    enhancedPrompt.append(image.getCategory().getText());
                    enhancedPrompt.append(":");
                    enhancedPrompt.append(image.getDescription());
                    enhancedPrompt.append("(").append(image.getUrl()).append(")\n");
                }
            }else enhancedPrompt.append(context.getImageListStr());

            context.setEnhancedPrompt(enhancedPrompt.toString());
            context.setCurrentStep("提示词增强");
            log.info("写入增强提示词");
            return WorkFlowContext.saveContext(context);
        });
    }
}
