package com.yzy.ai.context;

import com.yzy.ai.tools.WorkspaceResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工具输出存储分离：完整输出落盘，对话里只留截断版 + 回取路径。
 * <p>
 * 完整输出写入工作区 log/ 目录，模型看到的是"前 N 个字符 + [完整日志路径]"。
 * 前端展示时可按需读取完整日志，完全绕开 context 约束。
 */
@Slf4j
@Component
public class ToolOutputArchiver {

    private static final String LOG_SUBDIR = "log";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Autowired
    private WorkspaceResolver workspaceResolver;

    @Autowired
    private ContextCompressionProperties config;

    /**
     * 如果工具输出超过存储预算，归档完整输出并返回截断版。
     * 未超预算或工具不可压缩时，原样返回。
     *
     * @param appId    应用 ID
     * @param toolName 工具名称
     * @param output   工具的完整输出
     * @return 可能被截断的输出（含归档路径引用）
     */
    public String archiveIfNeeded(long appId, String toolName, String output) {
        if (output == null || output.isEmpty()) return output;
        if (!ToolCompressionPolicy.isCompressible(toolName)) return output;

        int budget = ToolCompressionPolicy.getBudget(toolName, config);
        if (output.length() <= budget) return output;

        String archivePath = writeToFile(appId, toolName, output);
        if (archivePath == null) {
            return truncate(output, budget);
        }
        return truncateWithPath(output, budget, archivePath);
    }

    private String writeToFile(long appId, String toolName, String content) {
        try {
            Path logDir = workspaceResolver.getRoot(appId).resolve(LOG_SUBDIR);
            Files.createDirectories(logDir);

            String filename = String.format("tool_%s_%s.log", LocalDateTime.now().format(TS_FORMAT), toolName);
            Path logFile = logDir.resolve(filename);
            Files.writeString(logFile, content, StandardCharsets.UTF_8);

            return LOG_SUBDIR + "/" + filename;
        } catch (IOException e) {
            log.warn("工具输出归档失败 appId={}, tool={}: {}", appId, toolName, e.getMessage());
            return null;
        }
    }

    private static String truncate(String output, int budget) {
        return output.substring(0, budget) + "\n...[输出已截断]";
    }

    private static String truncateWithPath(String output, int budget, String archivePath) {
        return output.substring(0, budget) + "\n...[输出已截断，完整日志: " + archivePath + "]";
    }
}
