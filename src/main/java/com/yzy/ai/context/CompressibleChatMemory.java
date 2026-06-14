package com.yzy.ai.context;

import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 可压缩对话记忆，实现分层上下文压缩策略。
 * <p>
 * 替代 MessageWindowChatMemory，核心差异：
 * <ul>
 *   <li>add() 时做存储分离：超预算工具输出归档到磁盘</li>
 *   <li>messages() 时按 token 占比动态决定压缩层级（Tier 0-2）</li>
 *   <li>保护区内的消息不受任何压缩影响</li>
 * </ul>
 */
@Slf4j
public class CompressibleChatMemory implements dev.langchain4j.memory.ChatMemory {

    private final Long appId;
    private final ChatMemoryStore store;
    private final ContextCompressionProperties config;
    private final TokenTracker tokenTracker;
    private final ToolOutputArchiver archiver;

    private static final int MAX_STORED_MESSAGES = 100;

    public CompressibleChatMemory(Long appId,
                                   ChatMemoryStore store,
                                   ContextCompressionProperties config,
                                   TokenTracker tokenTracker,
                                   ToolOutputArchiver archiver) {
        this.appId = appId;
        this.store = store;
        this.config = config;
        this.tokenTracker = tokenTracker;
        this.archiver = archiver;
    }

    @Override
    public Object id() {
        return appId;
    }

    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = new ArrayList<>(store.getMessages(appId));

        if (message instanceof SystemMessage sys) {
            // SystemMessage 唯一，替换已有的
            messages.removeIf(m -> m instanceof SystemMessage);
            messages.addFirst(sys);
        } else {
            // 存储分离：工具输出超预算时归档
            ChatMessage toStore = maybeArchive(message);
            messages.add(toStore);
        }

        evictIfNeeded(messages);
        cleanOrphanedToolResults(messages);
        store.updateMessages(appId, messages);
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> messages = store.getMessages(appId);
        if (messages.isEmpty() || !config.isEnabled()) {
            return messages;
        }

        int currentTokens = estimateCurrentTokens(messages);
        double ratio = (double) currentTokens / config.getEffectiveBudget();
        CompressionTier tier = CompressionTier.fromRatio(ratio, config.getTiers());

        if (tier == CompressionTier.NONE) {
            return messages;
        }

        // Tier 3 (SUMMARIZE) 属于方案 B，当前仅记录日志，降级到 PRUNE
        if (tier == CompressionTier.SUMMARIZE) {
            log.warn("appId={} 上下文占比 {}% 触发 Tier 3 (SUMMARIZE)，当前未实现，降级到 PRUNE",
                    appId, String.format("%.1f", ratio * 100));
            tier = CompressionTier.PRUNE;
        }

        int protectionStart = findProtectionZoneStart(messages);
        log.info("appId={} 上下文压缩: tier={}, tokens≈{}, ratio={}%, protectedFrom={}",
                appId, tier, currentTokens, String.format("%.1f", ratio * 100), protectionStart);

        return MessageCompressor.compress(messages, tier, protectionStart);
    }

    @Override
    public void clear() {
        store.updateMessages(appId, List.of());
        tokenTracker.remove(appId);
    }

    /**
     * 返回 store 中的原始消息（未经压缩），供 sanitizeMemory 等外部清理逻辑使用。
     */
    public List<ChatMessage> rawMessages() {
        return new ArrayList<>(store.getMessages(appId));
    }

    /**
     * 混合 token 估算：优先用 API 精确基准 + 增量估算，无历史数据时降级到纯字符估算。
     */
    private int estimateCurrentTokens(List<ChatMessage> messages) {
        TokenTracker.Snapshot snapshot = tokenTracker.getSnapshot(appId);
        if (snapshot == null) {
            return TokenEstimator.estimateTokens(messages);
        }

        int currentMsgCount = messages.size();
        int snapshotMsgCount = snapshot.messageCount();

        if (currentMsgCount <= snapshotMsgCount) {
            // 消息被清理过（或未新增），直接用 API 基准
            return (int) snapshot.inputTokens();
        }

        // API 基准 + 新增消息的字符估算
        List<ChatMessage> delta = messages.subList(snapshotMsgCount, currentMsgCount);
        int deltaTokens = TokenEstimator.estimateTokens(delta);
        return (int) snapshot.inputTokens() + deltaTokens;
    }

    /**
     * 从最后一条消息向前累计 token，直到达到保护区大小，返回保护区起始索引。
     */
    private int findProtectionZoneStart(List<ChatMessage> messages) {
        int protectionTokens = config.getProtectionZoneTokens();
        int accumulated = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            accumulated += TokenEstimator.estimateTokens(TokenEstimator.extractText(messages.get(i)));
            if (accumulated >= protectionTokens) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 对工具输出做存储分离：超预算时归档到磁盘，返回截断版。
     */
    private ChatMessage maybeArchive(ChatMessage message) {
        if (!(message instanceof ToolExecutionResultMessage tool)) return message;

        String text = tool.text();
        if (text == null || text.isEmpty()) return message;
        if (!ToolCompressionPolicy.isCompressible(tool.toolName())) return message;

        String archived = archiver.archiveIfNeeded(appId, tool.toolName(), text);
        if (archived.equals(text)) return message;

        return new ToolExecutionResultMessage(tool.id(), tool.toolName(), archived);
    }

    /**
     * 存储安全阀：超过上限时驱逐最老的非 SystemMessage 消息。
     */
    private void evictIfNeeded(List<ChatMessage> messages) {
        while (messages.size() > MAX_STORED_MESSAGES) {
            int removeIdx = (messages.getFirst() instanceof SystemMessage) ? 1 : 0;
            messages.remove(removeIdx);
        }
    }

    /**
     * 清理孤立的工具调用结果：如果 AiMessage 的 tool_calls 没有对应的 ToolExecutionResultMessage，
     * 则移除该 AiMessage 避免 API 报错。
     * 逻辑复用自 CodingAgentServiceFactory.sanitizeMemory()
     */
    private void cleanOrphanedToolResults(List<ChatMessage> messages) {
        if (messages.isEmpty()) return;
        ChatMessage last = messages.getLast();
        if (!(last instanceof AiMessage ai) || !ai.hasToolExecutionRequests()) return;

        Set<String> callIds = ai.toolExecutionRequests().stream()
                .map(r -> r.id())
                .collect(Collectors.toSet());

        int aiIdx = messages.size() - 1;
        for (int j = aiIdx + 1; j < messages.size(); j++) {
            if (messages.get(j) instanceof ToolExecutionResultMessage res) {
                callIds.remove(res.id());
            }
        }

        // 最后一条是带 tool_calls 的 AiMessage 但缺少对应结果 → 正常情况，工具正在执行
        // 不需要清理，LangChain4j 会在工具执行后补上 ToolExecutionResultMessage
    }
}
