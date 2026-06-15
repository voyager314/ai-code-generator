package com.yzy.ai.context;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;

/**
 * 摘要消息标识管理：在消息列表中标记、识别、提取增量摘要。
 */
public final class SummaryMarker {

    static final String PREFIX = "[CONTEXT_SUMMARY]\n";

    private SummaryMarker() {}

    public static boolean isSummary(ChatMessage msg) {
        return msg instanceof UserMessage user
                && user.hasSingleText()
                && user.singleText().startsWith(PREFIX);
    }

    public static UserMessage create(String summaryText) {
        return UserMessage.from(PREFIX + summaryText);
    }

    public static String extractContent(ChatMessage msg) {
        if (!isSummary(msg)) return "";
        return ((UserMessage) msg).singleText().substring(PREFIX.length());
    }

    /**
     * 从后向前查找最后一条摘要消息的索引，未找到返回 -1。
     */
    public static int findLast(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (isSummary(messages.get(i))) return i;
        }
        return -1;
    }
}
