package com.yzy.ai.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class PackageManagerTool extends BaseTool {
    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private PackageManagerDetector detector;

    @Tool("查看项目的包信息，包括脚本、依赖和检测到的包管理器")
    public String packageInfo(
            @P("操作类型: scripts | dependencies | devDependencies | all") String action,
            @ToolMemoryId long appId) {
        Path root = workspaceResolver.getRoot(appId);
        Path packageJsonPath = root.resolve("package.json");
        if (!Files.exists(packageJsonPath)) {
            throw new ToolExecutionException("未找到 package.json");
        }

        try {
            String content = Files.readString(packageJsonPath, StandardCharsets.UTF_8);
            JSONObject pkg = JSONUtil.parseObj(content);
            PackageManager pm = detector.detect(root);
            StringBuilder sb = new StringBuilder();

            String act = (action == null || action.isBlank()) ? "all" : action.strip().toLowerCase();

            if ("all".equals(act)) {
                sb.append("包名: ").append(pkg.getStr("name", "unknown")).append("\n");
                sb.append("版本: ").append(pkg.getStr("version", "0.0.0")).append("\n");
                sb.append("包管理器: ").append(pm.getDisplayName()).append("\n\n");
                appendScripts(sb, pkg);
                appendDeps(sb, pkg, "dependencies");
                appendDeps(sb, pkg, "devDependencies");
            } else if ("scripts".equals(act)) {
                appendScripts(sb, pkg);
            } else if ("dependencies".equals(act)) {
                appendDeps(sb, pkg, "dependencies");
            } else if ("devdependencies".equals(act)) {
                appendDeps(sb, pkg, "devDependencies");
            } else {
                throw new ToolExecutionException("未知操作类型: " + action + "，支持: scripts, dependencies, devDependencies, all");
            }

            return sb.toString();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("读取 package.json 失败: " + e.getMessage());
        }
    }

    private void appendScripts(StringBuilder sb, JSONObject pkg) {
        JSONObject scripts = pkg.getJSONObject("scripts");
        sb.append("--- scripts ---\n");
        if (scripts == null || scripts.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            scripts.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        sb.append("\n");
    }

    private void appendDeps(StringBuilder sb, JSONObject pkg, String key) {
        JSONObject deps = pkg.getJSONObject(key);
        sb.append("--- ").append(key).append(" ---\n");
        if (deps == null || deps.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            deps.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        sb.append("\n");
    }

    @Override
    String getToolName() {
        return "packageInfo";
    }
}
