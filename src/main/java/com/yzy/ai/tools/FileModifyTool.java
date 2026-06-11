package com.yzy.ai.tools;

import cn.hutool.core.io.FileUtil;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class FileModifyTool extends BaseTool{
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Tool("修改指定路径的文件")
    public String modifyFile(@P("文件相对路径") String relativePath,
                             @P("要修改的旧内容") String oldContent,
                             @P("替换后的新内容") String newConten,
                             @ToolMemoryId long appId){
        Path path = workspaceResolver.resolve(appId, relativePath);
        if(!Files.exists(path)||!Files.isRegularFile(path)){
            log.error("文件不存在或非文件: {}", relativePath);
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        String origin = FileUtil.readString(path.toFile(), StandardCharsets.UTF_8);
        if(!origin.contains(oldContent)){
            log.warn("不存在要替换的内容");
            return relativePath;
        }
        String modified = origin.replace(oldContent, newConten);
        if(modified.equals(origin)){
            log.warn("文件未发生变化");
            return relativePath;
        }
        try {
            Files.writeString(path,modified, StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("文件修改失败: {}, 路径: {}", e.getMessage(), relativePath);
            throw new ToolExecutionException("无法修改文件: " + e.getMessage());
        }
        log.info("文件修改成功");
        return relativePath;
    }

    @Override
    String getToolName() {
        return "modifyFile";
    }
}
