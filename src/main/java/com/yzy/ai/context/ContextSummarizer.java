package com.yzy.ai.context;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import com.yzy.ai.tools.WorkspaceResolver;
import dev.langchain4j.data.message.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Tier 3 增量摘要器：调用 DashScope 小模型将 delta 消息压缩为结构化摘要。
 */
@Slf4j
@Component
public class ContextSummarizer {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.abstract-model-url}")
    private String modelUrl;

    @Value("${dashscope.abstract-model}")
    private String modelName;

    @Autowired
    private WorkspaceResolver workspaceResolver;

    private String systemPrompt;

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @PostConstruct
    void loadPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompt/context-summary-prompt.md");
            systemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载摘要 prompt 失败，Tier 3 将不可用", e);
            systemPrompt = null;
        }
    }

    public boolean isAvailable() {
        return systemPrompt != null && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 对 delta 消息做增量摘要，返回结构化摘要文本。
     *
     * @param appId       应用 ID（用于落盘路径）
     * @param lastSummary 上次摘要内容，首次为空串
     * @param delta       需要压缩的消息列表
     * @return 合并后的摘要文本
     */
    public String summarize(long appId, String lastSummary, List<ChatMessage> delta) {
        String userContent = buildUserMessage(lastSummary, delta);

        Message sysMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemPrompt)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userContent)
                .build();

        Generation gen = new Generation(Protocol.HTTP.getValue(), modelUrl);
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(modelName)
                .messages(Arrays.asList(sysMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        try {
            GenerationResult result = gen.call(param);
            String summary = result.getOutput().getChoices().get(0).getMessage().getContent();
            log.info("Tier 3 摘要完成: appId={}, deltaSize={}, summaryLen={}",
                    appId, delta.size(), summary.length());
            persistSummary(appId, summary, delta.size());
            return summary;
        } catch (Exception e) {
            throw new RuntimeException("DashScope 摘要调用失败: " + e.getMessage(), e);
        }
    }

    private String buildUserMessage(String lastSummary, List<ChatMessage> delta) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Previous Summary\n");
        if (lastSummary == null || lastSummary.isBlank()) {
            sb.append("(none)\n");
        } else {
            sb.append(lastSummary).append("\n");
        }
        sb.append("\n## New Messages\n");
        for (ChatMessage msg : delta) {
            sb.append(formatMessage(msg)).append("\n");
        }
        return sb.toString();
    }

    private static String formatMessage(ChatMessage msg) {
        if (msg instanceof UserMessage user) {
            return "[USER] " + (user.hasSingleText() ? user.singleText() : user.contents().toString());
        }
        if (msg instanceof AiMessage ai) {
            StringBuilder sb = new StringBuilder("[ASSISTANT] ");
            if (ai.text() != null) sb.append(ai.text());
            if (ai.hasToolExecutionRequests()) {
                for (var req : ai.toolExecutionRequests()) {
                    sb.append("\n  -> tool_call: ").append(req.name());
                }
            }
            return sb.toString();
        }
        if (msg instanceof ToolExecutionResultMessage tool) {
            String text = tool.text();
            if (text != null && text.length() > 500) {
                text = text.substring(0, 500) + "...[truncated]";
            }
            return "[TOOL:" + tool.toolName() + "] " + text;
        }
        if (msg instanceof SystemMessage sys) {
            return "[SYSTEM] " + sys.text();
        }
        return "[UNKNOWN] " + msg;
    }

    private void persistSummary(long appId, String summary, int deltaSize) {
        try {
            Path root = workspaceResolver.getRoot(appId);
            Files.createDirectories(root);
            String filename = "summary_" + LocalDateTime.now().format(TS_FORMAT) + ".md";
            Path file = root.resolve(filename);
            String content = "# Context Summary\n\n"
                    + "- appId: " + appId + "\n"
                    + "- compressed: " + deltaSize + " messages\n"
                    + "- time: " + LocalDateTime.now() + "\n\n"
                    + summary;
            Files.writeString(file, content, StandardCharsets.UTF_8);
            log.debug("摘要已落盘: {}", file);
        } catch (IOException e) {
            log.warn("摘要落盘失败 appId={}: {}", appId, e.getMessage());
        }
    }
}
