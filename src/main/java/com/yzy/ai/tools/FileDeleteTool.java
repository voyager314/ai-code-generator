package com.yzy.ai.tools;

import cn.hutool.core.io.FileUtil;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class FileDeleteTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    private static final String[] ban={"package.json","package-lock.json",
            "yarn.lock","pnpm-lock.yaml",
            "vite.config.js","vite.config.ts",
    "vue.config.js","vue.config.ts","tsconfig.json","tsconfig.app.json","tsconfig.node.json",
    "index.html","main.ts","main.js","App.vue",".gitignore","README.md"};

    @Tool("删除指定路径的文件")
    public String deleteFile(@P("文件相对路径") String relativePath,@ToolMemoryId long appId){
        Path path = workspaceResolver.resolve(appId, relativePath);
        if(!Files.exists(path)||!Files.isRegularFile(path)){
            log.error("文件不存在或非文件: {}", relativePath);
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        //安全检查
        String fileName = path.getFileName().toString();
        for(String b:ban){
            if(fileName.equalsIgnoreCase(b)){
                log.warn("禁止删除关键文件: {}", fileName);
                throw new ToolExecutionException("禁止删除关键文件: " + fileName);
            }
        }
        try {
            FileUtil.del(path);
            log.info("文件删除成功: {}", relativePath);
            return "已删除: " + relativePath;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            throw new ToolExecutionException("无法删除文件: " + e.getMessage());
        }
    }

    @Override
    String getToolName() {
        return "deleteFile";
    }
}
