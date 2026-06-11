package com.yzy.ai.tools;

import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class CodeSearchTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    private static final int MAX_RESULTS = 30;
    private static final int MAX_PER_FILE = 5;

    private static final Set<String> IGNORE_DIRS = Set.of(
            "node_modules", ".git", "dist", "build", "target",
            ".idea", ".vscode", "coverage", "__pycache__", ".mvn");

    private static final Set<String> IGNORE_EXTENSIONS = Set.of(
            ".class", ".jar", ".war", ".exe", ".dll", ".so",
            ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
            ".woff", ".woff2", ".ttf", ".eot", ".map",
            ".lock", ".log");

    @Tool("在项目目录中搜索文件内容")
    public String searchCode(
            @P("搜索关键词") String pattern,
            @P("限定文件扩展名，如 .java .js .vue，留空搜索所有文本文件") String fileExtension,
            @ToolMemoryId long appId) {
        Path root = workspaceResolver.getRoot(appId);
        if (!Files.exists(root)) {
            throw new ToolExecutionException("工作空间不存在: " + root);
        }

        StringBuilder result = new StringBuilder();
        int[] totalCount = {0};

        try {
            searchDir(root.toFile(), root, pattern, fileExtension, result, totalCount);
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage());
            throw new ToolExecutionException("搜索失败: " + e.getMessage());
        }

        if (totalCount[0] == 0) {
            return "未找到匹配 \"" + pattern + "\" 的内容";
        }

        result.insert(0, "共找到 " + totalCount[0] + " 处匹配:\n\n");
        return result.toString();
    }

    private void searchDir(File dir, Path root, String pattern,
                           String fileExtension, StringBuilder result, int[] totalCount) {
        if (totalCount[0] >= MAX_RESULTS) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (totalCount[0] >= MAX_RESULTS) return;

            if (file.isDirectory()) {
                if (!IGNORE_DIRS.contains(file.getName())) {
                    searchDir(file, root, pattern, fileExtension, result, totalCount);
                }
                continue;
            }

            String name = file.getName();
            if (IGNORE_EXTENSIONS.stream().anyMatch(name::endsWith)) continue;
            if (fileExtension != null && !fileExtension.isBlank() && !name.endsWith(fileExtension)) continue;

            searchFile(file, root, pattern, result, totalCount);
        }
    }

    private void searchFile(File file, Path root, String pattern,
                            StringBuilder result, int[] totalCount) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String relativePath = root.relativize(file.toPath()).toString();
            int matchInFile = 0;

            for (int i = 0; i < lines.size(); i++) {
                if (totalCount[0] >= MAX_RESULTS) return;
                if (matchInFile >= MAX_PER_FILE) return;

                String line = lines.get(i);
                if (line.contains(pattern)) {
                    result.append(relativePath).append(":").append(i + 1).append(": ")
                            .append(line.strip()).append("\n");
                    totalCount[0]++;
                    matchInFile++;
                }
            }
        } catch (IOException e) {
            // skip binary or unreadable files
        }
    }

    @Override
    String getToolName() {
        return "searchCode";
    }
}
