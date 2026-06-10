package com.yzy.ai.tools;

import cn.hutool.core.io.FileUtil;
import com.yzy.common.AppConstant;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class FileDirReadTool extends BaseTool {
    /*
    需要忽略的文件和目录
     */
    private static final Set<String> IGNORE = Set.of(".idea",".vscode","node_modules",".git",".env","dist",
            ".DS_Store","target",".mvn","coverage");

    /*
    需要忽略的扩展名
     */
    private static final Set<String> EXTENSIONS=Set.of(".log",".temp",".cache",".lock");

    /**
     * 读取指定路径的文件
     * @param relativePath 相对路径
     * @param appId 应用id
     * @return 项目结构
     */
    @Tool("读取指定路径的目录")
    public String readDir(@P("文件相对路径") String relativePath,@ToolMemoryId long appId){
        Path path = Paths.get(relativePath);
        if(!path.isAbsolute()){
            Path rootPath = Paths.get(AppConstant.OUTPUT_DIR, "vue_project_" + appId);
            path = rootPath.resolve(relativePath);
        }
        if(!Files.exists(path)){
            log.error("目录不存在: {}", relativePath);
            throw new ToolExecutionException("目录不存在: " + relativePath);
        }

        try {
            File file = path.toFile();
            //使用hutool递归遍历所以文件
            List<File> files = FileUtil.loopFiles(file, f ->
                    !IGNORE.contains(f.getName())&&EXTENSIONS.stream().noneMatch(f.getName()::endsWith));
            StringBuilder sb = new StringBuilder();
            sb.append("项目目录结构:\n");
            files.stream().sorted((f1,f2)->{
                //先按深度排序文件，再按文件名排序
                int d1=getRelativDepth(file,f1);
                int d2=getRelativDepth(file,f2);
                if(d1!=d2)return Integer.compare(d1,d2);
                return f1.getPath().compareTo(f2.getPath());
            }).forEach(f -> {
                int d=getRelativDepth(file,f);
                String indent=" ".repeat(d);
                sb.append(indent).append(f.getName()).append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("读取目录失败: {}", e.getMessage());
            throw new ToolExecutionException("无法读取目录: " + e.getMessage());
        }
    }

    /**
     * 获取当前文件相对根目录的深度
     * @param root 根目录
     * @param file 当前文件
     * @return 相对深度
     */
    private int getRelativDepth(File root,File file){
        Path p1 = root.toPath();
        Path p2 = file.toPath();
        return p1.relativize(p2).getNameCount()-1;
    }

    @Override
    String getToolName() {
        return "readDir";
    }
}
