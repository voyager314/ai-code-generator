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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileModifyTool extends BaseTool{
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Tool("修改指定路径的文件，直接字符串匹配替换")
    public String modifyFile(@P("文件相对路径") String relativePath,
                             @P("要修改的旧内容") String oldContent,
                             @P("替换后的新内容") String newContent,
                             @ToolMemoryId long appId) {
        Path path = workspaceResolver.resolve(appId, relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.error("文件不存在或非文件: {}", relativePath);
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        String origin = FileUtil.readString(path.toFile(), StandardCharsets.UTF_8);
        if (!origin.contains(oldContent)) {
            log.warn("不存在要替换的内容");
            return relativePath;
        }
        String modified = origin.replace(oldContent, newContent);
        if (modified.equals(origin)) {
            log.warn("文件未发生变化");
            return relativePath;
        }
        try {
            Files.writeString(path, modified, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("文件修改失败: {}, 路径: {}", e.getMessage(), relativePath);
            throw new ToolExecutionException("无法修改文件: " + e.getMessage());
        }
        log.info("文件修改成功");
        return relativePath;
    }

    @Tool("按行范围修改文件内容")
    public String modifyFileByLine(@P("文件相对路径") String relativePath,
                                   @P("起始行号（从1开始）") int startLine,
                                   @P("结束行号（包含）") int endLine,
                                   @P("替换后的新内容") String newContent,
                                   @ToolMemoryId long appId) {
        Path path = workspaceResolver.resolve(appId, relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int totalLines = lines.size();
            if (startLine < 1 || endLine < startLine || endLine > totalLines) {
                throw new ToolExecutionException(
                        String.format("行号范围无效: %d-%d，文件共 %d 行", startLine, endLine, totalLines));
            }
            List<String> newContentLines = newContent.lines().collect(Collectors.toList());
            String result = Stream.of(
                            lines.subList(0, startLine - 1).stream(),
                            newContentLines.stream(),
                            lines.subList(endLine, totalLines).stream())
                    .flatMap(s -> s)
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.writeString(path, result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("按行修改成功: {} L{}-{}", relativePath, startLine, endLine);
            return relativePath;
        } catch (ToolExecutionException e) {
            throw e;
        } catch (IOException e) {
            throw new ToolExecutionException("文件修改失败: " + e.getMessage());
        }
    }

    @Tool("使用正则表达式修改文件内容")
    public String modifyFileByRegex(@P("文件相对路径") String relativePath,
                                    @P("正则表达式模式") String regexPattern,
                                    @P("替换后的新内容，可使用 $1 等反向引用") String replacement,
                                    @ToolMemoryId long appId) {
        Path path = workspaceResolver.resolve(appId, relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ToolExecutionException("文件不存在或非文件: " + relativePath);
        }
        if (regexPattern.length() > 500) {
            throw new ToolExecutionException("正则表达式过长，最大 500 字符");
        }
        try {
            String origin = FileUtil.readString(path.toFile(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(origin);
            String modified = matcher.replaceAll(replacement);
            if (modified.equals(origin)) {
                log.warn("正则替换未匹配到内容");
                return relativePath;
            }
            Files.writeString(path, modified, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("正则修改成功: {}", relativePath);
            return relativePath;
        } catch (PatternSyntaxException e) {
            throw new ToolExecutionException("正则表达式语法错误: " + e.getMessage());
        } catch (IOException e) {
            throw new ToolExecutionException("文件修改失败: " + e.getMessage());
        }
    }

    @Override
    String getToolName() {
        return "modifyFile";
    }
}
