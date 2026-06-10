package com.yzy.ai.WorkFlow.node;

import com.yzy.ai.ImageCollectionService;
import com.yzy.ai.WorkFlow.model.ImageCollectionPlan;
import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.WorkFlow.tools.ImageSearchTool;
import com.yzy.ai.WorkFlow.tools.LogoGenerateTool;
import com.yzy.ai.WorkFlow.tools.MermaidTool;
import com.yzy.ai.WorkFlow.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ImageCollect {
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            ImageCollectionService service = SpringContextUtil.getBean(ImageCollectionService.class);
            String originalPrompt = context.getOriginalPrompt();
            String imgListStr = service.collectImages(originalPrompt);

            ImageCollectionPlan plan = service.planImageCollection(originalPrompt);
            List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
            ImageSearchTool searchTool = SpringContextUtil.getBean(ImageSearchTool.class);
            //使用CompletableFuture并发执行图片收集任务
            if(plan.getContentImageTasks()!=null){
                plan.getContentImageTasks().forEach(task->{
                    futures.add(CompletableFuture.supplyAsync(()->searchTool.searchImages(task.query())));
                });
            }
            if(plan.getIllustrationTasks()!=null){
                plan.getIllustrationTasks().forEach(task->{
                    futures.add(CompletableFuture.supplyAsync(()->searchTool.searchImages(task.query())));
                });
            }
            if(plan.getDiagramTasks()!=null){
                MermaidTool mermaidTool = SpringContextUtil.getBean(MermaidTool.class);
                plan.getDiagramTasks().forEach(task->{
                    futures.add(CompletableFuture.supplyAsync(()->
                            mermaidTool.genDiagram(task.mermaidCode(),task.description())));
                });
            }
            if(plan.getLogoTasks()!=null){
                LogoGenerateTool logoGenerateTool = SpringContextUtil.getBean(LogoGenerateTool.class);
                plan.getLogoTasks().forEach(task->{
                    futures.add(CompletableFuture.supplyAsync(()->logoGenerateTool.getLogo(task.description())));
                });
            }
            List<ImageResource> images = new ArrayList<>();
            //等待所有任务执行完毕
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<List<ImageResource>> future : futures) {
                List<ImageResource> list = future.get();
                if(list!=null&&!list.isEmpty()){images.addAll(list);}
            }
            context.setImageList(images);
            context.setImageListStr(imgListStr);
            context.setCurrentStep("收集图片");
            log.info("图片收集完毕");
            return WorkFlowContext.saveContext(context);
        });
    }
}
