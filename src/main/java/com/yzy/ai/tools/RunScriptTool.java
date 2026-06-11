package com.yzy.ai.tools;

import cn.hutool.json.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RunScriptTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private PackageManagerDetector detector;

    @Autowired
    private ApprovalService approvalService;

    private static final long TIMEOUT_SECONDS = 1200;
    private static final int MAX_OUTPUT_LENGTH = 5000;
    private static final Pattern SHELL_META = Pattern.compile("[;|&$`><]");

    @Tool("运行项目 package.json 中定义的脚本")
    public String runScript(
            @P("脚本名称，如 dev, build, test, lint") String scriptName,
            @ToolMemoryId long appId) {
        if (scriptName == null || scriptName.isBlank()) {
            throw new ToolExecutionException("脚本名称不能为空");
        }
        if (SHELL_META.matcher(scriptName).find()) {
            throw new ToolExecutionException("脚本名称中包含非法字符");
        }

        Path root = workspaceResolver.getRoot(appId);
        if (!Files.exists(root)) {
            throw new ToolExecutionException("工作空间不存在: " + root);
        }

        verifyScriptExists(root, scriptName.strip());

        PackageManager pm = detector.detect(root);
        String command = pm.getRunPrefix() + " " + scriptName.strip();

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
                return output + "\n[脚本执行超时，已终止]";
            }

            int exitCode = process.exitValue();
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output.setLength(MAX_OUTPUT_LENGTH);
                output.append("\n...[输出已截断]");
            }

            return "退出码: " + exitCode + "\n" + output;
        } catch (Exception e) {
            log.error("脚本执行失败: {}", e.getMessage());
            throw new ToolExecutionException("脚本执行失败: " + e.getMessage());
        }
    }

    private void verifyScriptExists(Path root, String scriptName) {
        Path packageJsonPath = root.resolve("package.json");
        if (!Files.exists(packageJsonPath)) {
            throw new ToolExecutionException("未找到 package.json");
        }
        try {
            String content = Files.readString(packageJsonPath, StandardCharsets.UTF_8);
            JSONObject pkg = JSONUtil.parseObj(content);
            JSONObject scripts = pkg.getJSONObject("scripts");
            if (scripts == null || !scripts.containsKey(scriptName)) {
                StringBuilder hint = new StringBuilder("脚本 \"" + scriptName + "\" 不存在");
                if (scripts != null && !scripts.isEmpty()) {
                    hint.append("，可用脚本: ").append(String.join(", ", scripts.keySet()));
                }
                throw new ToolExecutionException(hint.toString());
            }
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("读取 package.json 失败，跳过脚本验证: {}", e.getMessage());
        }
    }

    private boolean requestApproval(long appId, String command) {
        FluxSink<String> sink = approvalService.getSink(appId);
        if (sink == null) {
            return true;
        }
        String approvalId = UUID.randomUUID().toString();
        AgentEvent event = AgentEvent.approvalRequest(approvalId, "即将执行: " + command);
        sink.next(JSONUtil.toJsonStr(event));
        boolean approved = approvalService.requestAndWait(approvalId);
        AgentEvent resultEvent = AgentEvent.approvalResult(approvalId, approved);
        sink.next(JSONUtil.toJsonStr(resultEvent));
        return approved;
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
        return "runScript";
    }
}
