package com.yzy.ai.tools;

import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CommandExecuteTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    private static final long TIMEOUT_SECONDS = 60;
    private static final int MAX_OUTPUT_LENGTH = 3000;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "npm", "node", "npx", "pnpm", "yarn",
            "python", "python3", "pip", "pip3",
            "mvn", "gradle", "java", "javac",
            "eslint", "prettier", "tsc",
            "ls", "dir", "cat", "type", "echo", "pwd",
            "mkdir", "touch", "cp", "mv",
            "git");

    private static final Set<String> BLOCKED_PATTERNS = Set.of(
            "rm -rf", "del /s", "format ", "shutdown",
            "curl ", "wget ", "ssh ", "scp ",
            "chmod 777", "sudo ", "su ",
            "> /dev", "| bash", "| sh",
            "powershell", "cmd /c del", "cmd /c format");

    @Tool("在项目目录中执行命令")
    public String executeCommand(
            @P("要执行的命令") String command,
            @ToolMemoryId long appId) {
        Path root = workspaceResolver.getRoot(appId);
        if (!Files.exists(root)) {
            throw new ToolExecutionException("工作空间不存在: " + root);
        }

        validateCommand(command);

        try {
            ProcessBuilder pb = buildProcess(command, root);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() < MAX_OUTPUT_LENGTH) {
                        output.append(line).append("\n");
                    }
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return output + "\n[命令执行超时，已终止]";
            }

            int exitCode = process.exitValue();
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output.setLength(MAX_OUTPUT_LENGTH);
                output.append("\n...[输出已截断]");
            }

            return "退出码: " + exitCode + "\n" + output;
        } catch (Exception e) {
            log.error("命令执行失败: {}", e.getMessage());
            throw new ToolExecutionException("命令执行失败: " + e.getMessage());
        }
    }

    private void validateCommand(String command) {
        String trimmed = command.strip().toLowerCase();

        for (String blocked : BLOCKED_PATTERNS) {
            if (trimmed.contains(blocked)) {
                throw new ToolExecutionException("禁止执行的命令模式: " + blocked);
            }
        }

        String firstToken = trimmed.split("\\s+")[0];
        // strip path prefixes like ./node_modules/.bin/eslint -> eslint
        if (firstToken.contains("/")) {
            firstToken = firstToken.substring(firstToken.lastIndexOf('/') + 1);
        }
        if (firstToken.contains("\\")) {
            firstToken = firstToken.substring(firstToken.lastIndexOf('\\') + 1);
        }
        // strip .exe/.cmd/.bat suffix on Windows
        firstToken = firstToken.replaceAll("\\.(exe|cmd|bat)$", "");

        if (!ALLOWED_COMMANDS.contains(firstToken)) {
            throw new ToolExecutionException("不允许执行的命令: " + firstToken
                    + "，允许的命令: " + ALLOWED_COMMANDS);
        }
    }

    private ProcessBuilder buildProcess(String command, Path workDir) {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", command);
        } else {
            pb = new ProcessBuilder("sh", "-c", command);
        }
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        return pb;
    }

    @Override
    String getToolName() {
        return "executeCommand";
    }
}
