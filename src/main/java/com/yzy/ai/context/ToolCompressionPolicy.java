package com.yzy.ai.context;

import java.util.Set;

/**
 * 工具压缩策略：决定哪些工具的输出可以被压缩/归档。
 * <p>
 * 三个类别：
 * <ul>
 *   <li>PROTECTED — 输出有状态意义，任何 tier 都不动（Task/Skill，待引入后填充）</li>
 *   <li>WRITE_TOOLS — 写入类工具，输出通常很短（成功/失败），不需要归档</li>
 *   <li>COMPRESSIBLE — 无状态读取类工具，输出可能很大，是压缩的主力</li>
 * </ul>
 */
public final class ToolCompressionPolicy {

    // TODO: 待 Task/Skill 机制引入后填充
    private static final Set<String> PROTECTED_TOOLS = Set.of();

    private static final Set<String> WRITE_TOOLS = Set.of(
            "writeFile", "modifyFile", "modifyFileByLine", "modifyFileByRegex",
            "deleteFile", "installPackage"
    );

    private static final Set<String> COMPRESSIBLE_TOOLS = Set.of(
            "executeCommand", "runScript", "readFile", "readDir", "searchCode", "packageInfo"
    );

    private static final int DEFAULT_BUDGET = 3000;

    private ToolCompressionPolicy() {}

    public static boolean isProtected(String toolName) {
        return PROTECTED_TOOLS.contains(toolName);
    }

    public static boolean isCompressible(String toolName) {
        return COMPRESSIBLE_TOOLS.contains(toolName);
    }

    /**
     * 获取工具的存储预算（字符数）。
     * 优先从 ContextCompressionProperties 动态配置读取，未配置则返回默认值。
     */
    public static int getBudget(String toolName, ContextCompressionProperties config) {
        if (config == null) return DEFAULT_BUDGET;
        return config.getToolBudget(toolName, DEFAULT_BUDGET);
    }
}
