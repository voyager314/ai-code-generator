package com.yzy.ai.context;

import dev.langchain4j.data.message.*;

import java.util.List;

/**
 * 基于字符数的 token 估算器。
 * <p>
 * DeepSeek 无公开 Java tokenizer，使用字符数估算。
 * charsPerToken 可通过 API 返回的实际 token 数在运行时校准。
 * 对于 tier 判定（阈值间隔 20%），±15% 的估算误差不影响决策。
 */
public final class TokenEstimator {

    private static volatile double charsPerToken = 3.5;

    private TokenEstimator() {}

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / charsPerToken);
    }

    public static int estimateTokens(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage msg : messages) {
            total += estimateTokens(extractText(msg));
        }
        return total;
    }

    /**
     * 用 API 返回的实际 token 数校准 charsPerToken 比率。
     * 采用指数移动平均（alpha=0.3）避免单次异常值导致大幅波动。
     */
    public static void calibrate(long apiTokenCount, int charCount) {
        if (apiTokenCount <= 0 || charCount <= 0) return;
        double measured = (double) charCount / apiTokenCount;
        charsPerToken = charsPerToken * 0.7 + measured * 0.3;
    }

    public static double getCharsPerToken() {
        return charsPerToken;
    }

    public static String extractText(ChatMessage message) {
        if (message instanceof SystemMessage sys) {
            return sys.text();
        }
        if (message instanceof AiMessage ai) {
            StringBuilder sb = new StringBuilder();
            if (ai.text() != null) sb.append(ai.text());
            if (ai.hasToolExecutionRequests()) {
                for (var req : ai.toolExecutionRequests()) {
                    sb.append(req.name());
                    if (req.arguments() != null) sb.append(req.arguments());
                }
            }
            return sb.toString();
        }
        if (message instanceof UserMessage user) {
            return user.hasSingleText() ? user.singleText() : user.contents().toString();
        }
        if (message instanceof ToolExecutionResultMessage tool) {
            return tool.text();
        }
        return message.toString();
    }
}
