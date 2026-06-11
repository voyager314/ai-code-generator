package com.yzy.ai.tools;

import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class FileReadTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Tool("读取指定路径的文件")
    public String readFile(@P("文件相对路径") String relativePath, @ToolMemoryId long appId){
        Path path = workspaceResolver.resolve(appId, relativePath);
        if(!Files.exists(path)||!Files.isRegularFile(path)){
            log.error("文件不存在或非文件: {}", relativePath);
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.error("文件读取失败: {}, 路径: {}", e.getMessage(), relativePath);
            throw new ToolExecutionException("无法读取文件: " + e.getMessage());
        }
    }

    @Override
    String getToolName() {
        return "readFile";
    }
}
