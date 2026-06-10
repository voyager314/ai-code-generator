package com.yzy.ai.WorkFlow.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.system.SystemUtil;
import com.yzy.ai.WorkFlow.model.ImageCategoryEnum;
import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.exception.ToolExecutionException;
import com.yzy.manager.AliOSSManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MermaidTool {
    @Autowired
    private AliOSSManager manager;

    @Tool("将mermaid代码转为架构图，用于展示系统架构和技术关系")
    public List<ImageResource> genDiagram(@P("mermaid图表代码") String mermaidCode, @P("架构图") String description){
        List<ImageResource> res = new ArrayList<>();
        File diagramFile = convertMermaidToSvg(mermaidCode);
        String url = manager.upload(diagramFile.getPath());
        FileUtil.del(diagramFile);
        res.add(ImageResource.builder()
                .category(ImageCategoryEnum.ARCHITECTURE)
                .description(description)
                .url(url)
                .build());
        return res;
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // 根据操作系统选择命令
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        // 构建命令
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                command,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );
        // 执行命令
        try {
            RuntimeUtil.execForStr(cmdLine);
            if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
                throw new ToolExecutionException(
                    "架构图生成失败：Mermaid CLI执行失败。请确保已安装Mermaid CLI (npm install -g @mermaid-js/mermaid-cli)"
                );
            }
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Mermaid转换失败: {}", e.getMessage());
            throw new ToolExecutionException("架构图生成失败: " + e.getMessage());
        } finally {
            // 清理输入文件，保留输出文件供上传使用
            FileUtil.del(tempInputFile);
        }
        return tempOutputFile;
    }
}
