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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
public class InstallPackageTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private PackageManagerDetector detector;

    @Autowired
    private ApprovalService approvalService;

    private static final long TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_LENGTH = 5000;
    private static final Pattern SHELL_META = Pattern.compile("[;|&$`><]");

    @Tool("安装npm包，自动检测包管理器")
    public String installPackage(
            @P("包名，多个用空格分隔") String packages,
            @P("是否安装为开发依赖") boolean isDev,
            @ToolMemoryId long appId) {
        if (packages == null || packages.isBlank()) {
            throw new ToolExecutionException("包名不能为空");
        }
        if (SHELL_META.matcher(packages).find()) {
            throw new ToolExecutionException("包名中包含非法字符");
        }

        Path root = workspaceResolver.getRoot(appId);
        if (!Files.exists(root)) {
            throw new ToolExecutionException("工作空间不存在: " + root);
        }

        PackageManager pm = detector.detect(root);
        String command = pm.getAddCommandWithDev(isDev) + " " + packages.strip();

        String rejection = requestApproval(appId, command);
        if (rejection != null) {
            return rejection;
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
                return output + "\n[安装超时，已终止]";
            }

            int exitCode = process.exitValue();
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output.setLength(MAX_OUTPUT_LENGTH);
                output.append("\n...[输出已截断]");
            }

            return "退出码: " + exitCode + "\n" + output;
        } catch (Exception e) {
            log.error("安装失败: {}", e.getMessage());
            throw new ToolExecutionException("安装失败: " + e.getMessage());
        }
    }

    private String requestApproval(long appId, String command) {
        FluxSink<String> sink = approvalService.getSink(appId);
        if (sink == null) {
            return null;
        }
        String approvalId = UUID.randomUUID().toString();
        AgentEvent event = AgentEvent.approvalRequest(approvalId, "即将执行: " + command);
        sink.next(JSONUtil.toJsonStr(event));
        ApprovalService.ApprovalResult result = approvalService.requestAndWait(approvalId);
        AgentEvent resultEvent = AgentEvent.approvalResult(approvalId, result.approved());
        sink.next(JSONUtil.toJsonStr(resultEvent));
        if (result.approved()) {
            return null;
        }
        String msg = result.customMessage();
        return (msg != null && !msg.isBlank())
                ? "[用户拒绝执行该命令。用户反馈: " + msg + "]"
                : "[用户拒绝执行该命令]";
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
        return "installPackage";
    }
}
