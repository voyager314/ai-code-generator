package com.yzy.ai.context;

import dev.langchain4j.data.message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier 1 (SNIP) 和 Tier 2 (PRUNE) 的压缩执行器，纯字符串处理，零 LLM 成本。
 * <p>
 * Tier 1 — 截断老工具输出，保留前几行 + 总量标注；截短用户消息中的代码块。
 * Tier 2 — 工具输出替换为占位符；裁剪 assistant 旧文本到前两句。
 */
public final class MessageCompressor {

    private static final int SNIP_TOOL_CHARS = 500;
    private static final int SNIP_CODE_LINES = 5;
    private static final int PRUNE_AI_CHARS = 200;
    private static final String COMPACTED_PLACEHOLDER = "[Content compacted to save space]";
    private static final Pattern CODE_BLOCK = Pattern.compile("(```\\w*\\n)(.*?)(```)", Pattern.DOTALL);

    private MessageCompressor() {}

    /**
     * 对保护区外的消息按 tier 压缩。
     *
     * @param messages        全部消息
     * @param tier            当前压缩层级
     * @param protectionStart 保护区起始索引（该索引及之后的消息不压缩）
     * @return 压缩后的消息列表（新列表，不修改原列表）
     */
    public static List<ChatMessage> compress(List<ChatMessage> messages, CompressionTier tier,
                                              int protectionStart) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (i >= protectionStart || msg instanceof SystemMessage) {
                result.add(msg);
            } else {
                result.add(compressOne(msg, tier));
            }
        }
        return result;
    }

    private static ChatMessage compressOne(ChatMessage message, CompressionTier tier) {
        if (message instanceof ToolExecutionResultMessage tool) {
            return compressTool(tool, tier);
        }
        if (message instanceof AiMessage ai) {
            return compressAi(ai, tier);
        }
        if (message instanceof UserMessage user && tier.ordinal() >= CompressionTier.SNIP.ordinal()) {
            return compressUser(user);
        }
        return message;
    }

    private static ChatMessage compressTool(ToolExecutionResultMessage tool, CompressionTier tier) {
        String text = tool.text();
        if (text == null || text.isEmpty()) return tool;

        if (tier.ordinal() >= CompressionTier.PRUNE.ordinal()) {
            return new ToolExecutionResultMessage(tool.id(), tool.toolName(), COMPACTED_PLACEHOLDER);
        }

        // SNIP: 保留前 N 个字符 + 标注
        if (text.length() > SNIP_TOOL_CHARS) {
            String snipped = text.substring(0, SNIP_TOOL_CHARS)
                    + "\n...[省略 " + (text.length() - SNIP_TOOL_CHARS) + " 字符]";
            return new ToolExecutionResultMessage(tool.id(), tool.toolName(), snipped);
        }
        return tool;
    }

    private static ChatMessage compressAi(AiMessage ai, CompressionTier tier) {
        if (tier.ordinal() < CompressionTier.PRUNE.ordinal()) return ai;

        // PRUNE: 裁剪 AI 文本到前两句
        String text = ai.text();
        if (text == null || text.length() <= PRUNE_AI_CHARS) return ai;

        String truncated = text.substring(0, PRUNE_AI_CHARS) + "...[truncated]";

        if (ai.hasToolExecutionRequests()) {
            return new AiMessage(truncated, ai.toolExecutionRequests());
        }
        return AiMessage.from(truncated);
    }

    private static ChatMessage compressUser(UserMessage user) {
        // SNIP: 只截短用户消息中的 markdown 代码块，纯文本指令不动
        if (!user.hasSingleText()) return user;
        String text = user.singleText();
        Matcher matcher = CODE_BLOCK.matcher(text);
        if (!matcher.find()) return user;

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        matcher.reset();
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String header = matcher.group(1);
            String code = matcher.group(2);
            String[] lines = code.split("\n", -1);
            if (lines.length > SNIP_CODE_LINES) {
                sb.append(header);
                for (int i = 0; i < SNIP_CODE_LINES; i++) {
                    sb.append(lines[i]).append("\n");
                }
                sb.append("...[共 ").append(lines.length).append(" 行，已省略]\n```");
            } else {
                sb.append(matcher.group(0));
            }
            lastEnd = matcher.end();
        }
        sb.append(text, lastEnd, text.length());

        String compressed = sb.toString();
        return compressed.equals(text) ? user : UserMessage.from(compressed);
    }
}
