package com.yzy.ai.tools;

import cn.hutool.json.JSONUtil;
import com.yzy.ai.approval.ApprovalService;
import com.yzy.ai.model.AgentEvent;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 命令执行工具
 * <p>
 * 在工作空间目录下执行 shell 命令，用于构建、测试、安装依赖等。
 * 安全机制：
 * - ALLOWED_COMMANDS 白名单：只允许已知安全的命令前缀
 * - BLOCKED_PATTERNS 黑名单：拦截 rm -rf、sudo 等危险模式
 * - 60 秒超时自动终止
 * - 输出截断到 3000 字符
 * <p>
 * HITL 集成：Agent 模式下执行前通过 ApprovalService 推送审批事件到 SSE 流，
 * 阻塞等待用户确认；非 Agent 模式（sink 未注册）自动放行。
 */
@Slf4j
@Component
public class CommandExecuteTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private ApprovalService approvalService;

    private static final long TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_LENGTH = 5000;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "npm", "node", "npx", "pnpm", "yarn",
            "vite", "vitest", "jest", "webpack", "esbuild",
            "turbo", "nx", "bun", "deno",
            "playwright", "cypress", "vue-tsc",
            "next", "nuxi", "astro",
            "tailwindcss", "prisma", "drizzle-kit",
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

        if (!requestApproval(appId, command)) {
            return "[用户拒绝执行该命令]";
        }

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

    /**
     * 通过 ApprovalService 请求用户审批。
     * 向 SSE 流推送 APPROVAL_REQUEST 事件，阻塞等待用户通过 REST 回调响应。
     * 无 sink（非 Agent 模式）时直接放行。
     */
    private boolean requestApproval(long appId, String command) {
        FluxSink<String> sink = approvalService.getSink(appId);
        if (sink == null) {
            return true;
        }

        String approvalId = UUID.randomUUID().toString();
        AgentEvent event = AgentEvent.approvalRequest(approvalId, "即将执行命令: " + command);
        sink.next(JSONUtil.toJsonStr(event));

        boolean approved = approvalService.requestAndWait(approvalId);

        AgentEvent resultEvent = AgentEvent.approvalResult(approvalId, approved);
        sink.next(JSONUtil.toJsonStr(resultEvent));

        return approved;
    }

    /**
     * 校验命令安全性：先检查黑名单模式，再检查命令前缀是否在白名单中。
     * 自动剥离路径前缀和 .exe/.cmd/.bat 后缀以适配 Windows。
     */
    private void validateCommand(String command) {
        String trimmed = command.strip().toLowerCase();

        for (String blocked : BLOCKED_PATTERNS) {
            if (trimmed.contains(blocked)) {
                throw new ToolExecutionException("禁止执行的命令模式: " + blocked);
            }
        }

        String firstToken = trimmed.split("\\s+")[0];
        if (firstToken.contains("/")) {
            firstToken = firstToken.substring(firstToken.lastIndexOf('/') + 1);
        }
        if (firstToken.contains("\\")) {
            firstToken = firstToken.substring(firstToken.lastIndexOf('\\') + 1);
        }
        firstToken = firstToken.replaceAll("\\.(exe|cmd|bat)$", "");

        if (!ALLOWED_COMMANDS.contains(firstToken)) {
            throw new ToolExecutionException("不允许执行的命令: " + firstToken
                    + "，允许的命令: " + ALLOWED_COMMANDS);
        }
    }

    /**
     * 构建跨平台进程：Windows 使用 cmd /c，Unix 使用 sh -c。
     * stderr 合并到 stdout，工作目录设为工作空间根。
     */
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
