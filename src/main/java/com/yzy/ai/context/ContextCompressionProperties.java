package com.yzy.ai.context;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文压缩配置。
 * <p>
 * models 表维护"model-name → context window limit"映射，可随时添加新模型。
 * effectiveBudget 为实际生效的上下文预算（token 数），tier 阈值基于此计算。
 * 设置远小于模型物理上限的有效预算可以更积极地控制成本和注意力质量。
 */
@Data
@Component
@ConfigurationProperties(prefix = "context-compression")
public class ContextCompressionProperties {

    private boolean enabled = true;

    /**
     * 保护区大小（token），保护区内的消息不会被任何 tier 压缩
     */
    private int protectionZoneTokens = 40000;

    /**
     * 有效上下文预算（token），tier 阈值以此为分母。
     */
    private int effectiveBudget = 32768;

    /**
     * 各 tier 触发阈值
     */
    private Tiers tiers = new Tiers();

    /**
     * 模型限制表：model-name → 模型参数
     */
    private Map<String, ModelLimit> models = new HashMap<>();

    /**
     * 工具存储预算：tool-name → 最大保留字符数（超出部分落盘归档）
     */
    private Map<String, Integer> toolBudgets = new HashMap<>();

    @Data
    public static class Tiers {
        private double snipThreshold = 0.6;
        private double pruneThreshold = 0.8;
        private double summarizeThreshold = 0.95;
    }

    @Data
    public static class ModelLimit {
        /**
         * 模型的物理 context window 上限（token 数）
         */
        private int contextWindow;
    }

    /**
     * 获取指定工具的存储预算（字符数），未配置则返回默认值
     */
    public int getToolBudget(String toolName, int defaultBudget) {
        return toolBudgets.getOrDefault(toolName, defaultBudget);
    }
}
