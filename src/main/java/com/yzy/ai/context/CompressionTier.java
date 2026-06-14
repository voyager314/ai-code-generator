package com.yzy.ai.context;

/**
 * 上下文压缩层级，按上下文占用比例递进触发
 */
public enum CompressionTier {
    NONE,
    SNIP,
    PRUNE,
    SUMMARIZE;

    public static CompressionTier fromRatio(double ratio, ContextCompressionProperties.Tiers tiers) {
        if (ratio >= tiers.getSummarizeThreshold()) return SUMMARIZE;
        if (ratio >= tiers.getPruneThreshold()) return PRUNE;
        if (ratio >= tiers.getSnipThreshold()) return SNIP;
        return NONE;
    }
}
