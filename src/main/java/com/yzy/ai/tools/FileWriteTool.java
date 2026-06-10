package com.yzy.ai.tools;

import com.yzy.common.AppConstant;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持AI通过工具调用写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool{
    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ){
        Path path = Paths.get(relativeFilePath);
        if(!path.isAbsolute()){
            Path projectRoot = Paths.get(AppConstant.OUTPUT_DIR, "vue_project_" + appId);
            path=projectRoot.resolve(relativeFilePath);
        }
        Path parent = path.getParent();
        //创建父目录（如果不存在）
        if(parent != null){
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                log.error("创建父目录失败: {}", e.getMessage());
                throw new ToolExecutionException("无法创建父目录: " + e.getMessage());
            }
        }
        try {
            Files.write(path,content.getBytes(), StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("文件写入失败: {}, 路径: {}", e.getMessage(), relativeFilePath);
            throw new ToolExecutionException("无法写入文件: " + e.getMessage());
        }
        //不要返回绝对路径，会暴露服务器存储地址
        return relativeFilePath;
    }

    @Override
    String getToolName() {
        return "writeFile";
    }
}
