package com.yzy.ai.context;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class CompressibleChatMemory implements ChatMemory {

    private final Long appId;
    private final ChatMemoryStore store;
    private final ContextCompressionProperties config;
    private final TokenTracker tokenTracker;
    private final ToolOutputArchiver archiver;
    private final ContextSummarizer contextSummarizer;

    private static final int MAX_STORED_MESSAGES = 100;
    private static final ConcurrentHashMap<Long, AtomicBoolean> SUMMARIZE_LOCKS = new ConcurrentHashMap<>();

    public CompressibleChatMemory(Long appId,
                                   ChatMemoryStore store,
                                   ContextCompressionProperties config,
                                   TokenTracker tokenTracker,
                                   ToolOutputArchiver archiver,
                                   ContextSummarizer contextSummarizer) {
        this.appId = appId;
        this.store = store;
        this.config = config;
        this.tokenTracker = tokenTracker;
        this.archiver = archiver;
        this.contextSummarizer = contextSummarizer;
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

        int protectionStart = findProtectionZoneStart(messages);

        if (tier == CompressionTier.SUMMARIZE) {
            // 先试 PRUNE（零成本），看能否救回来
            List<ChatMessage> pruned = MessageCompressor.compress(messages, CompressionTier.PRUNE, protectionStart);
            int prunedTokens = estimateCurrentTokens(pruned);
            double prunedRatio = (double) prunedTokens / config.getEffectiveBudget();

            if (prunedRatio >= config.getTiers().getSummarizeThreshold()) {
                // PRUNE 救不回来，调 LLM
                messages = trySummarize(messages, ratio, protectionStart);
                currentTokens = estimateCurrentTokens(messages);
                ratio = (double) currentTokens / config.getEffectiveBudget();
                tier = CompressionTier.fromRatio(ratio, config.getTiers());
                protectionStart = findProtectionZoneStart(messages);
                if (tier == CompressionTier.SUMMARIZE) {
                    tier = CompressionTier.PRUNE;
                }
                if (tier == CompressionTier.NONE) {
                    return messages;
                }
            } else {
                // PRUNE 够用，跳过 LLM
                log.info("appId={} PRUNE 已将 ratio 从 {}% 降至 {}%，跳过 Tier 3",
                        appId, String.format("%.1f", ratio * 100), String.format("%.1f", prunedRatio * 100));
                tier = CompressionTier.fromRatio(prunedRatio, config.getTiers());
            }
        }

        log.info("appId={} 上下文压缩: tier={}, tokens≈{}, ratio={}%, protectedFrom={}",
                appId, tier, currentTokens, String.format("%.1f", ratio * 100), protectionStart);

        return MessageCompressor.compress(messages, tier, protectionStart);
    }

    @Override
    public void clear() {
        store.deleteMessages(appId);
        tokenTracker.remove(appId);
    }

    /**
     * 返回 store 中的原始消息（未经压缩），供 sanitizeMemory 等外部清理逻辑使用。
     */
    public List<ChatMessage> rawMessages() {
        return new ArrayList<>(store.getMessages(appId));
    }

    /**
     * 批量替换 store 中的消息，单次 Redis 写入，供 sanitizeMemory 使用。
     */
    public void replaceAll(List<ChatMessage> messages) {
        store.updateMessages(appId, messages);
    }

    /**
     * 尝试执行 Tier 3 增量摘要。成功则返回摘要后的消息列表，失败则返回原列表。
     */
    private List<ChatMessage> trySummarize(List<ChatMessage> messages, double ratio, int protectionStart) {
        if (contextSummarizer == null || !contextSummarizer.isAvailable()) {
            log.warn("appId={} 触发 Tier 3 ({}%) 但摘要器不可用，降级 PRUNE",
                    appId, String.format("%.1f", ratio * 100));
            return messages;
        }

        AtomicBoolean lock = SUMMARIZE_LOCKS.computeIfAbsent(appId, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            log.warn("appId={} Tier 3 摘要进行中，本次降级 PRUNE", appId);
            return messages;
        }

        try {
            int summaryIdx = SummaryMarker.findLast(messages);
            String lastSummary = summaryIdx >= 0
                    ? SummaryMarker.extractContent(messages.get(summaryIdx)) : "";

            // delta = 上次摘要之后 ~ 保护区之前
            int deltaStart = summaryIdx + 1;
            // 跳过 SystemMessage
            if (deltaStart < messages.size() && messages.get(deltaStart) instanceof SystemMessage) {
                deltaStart++;
            }
            if (deltaStart >= protectionStart) {
                log.info("appId={} Tier 3 无可摘要的 delta 消息，降级 PRUNE", appId);
                return messages;
            }

            List<ChatMessage> delta = messages.subList(deltaStart, protectionStart);
            if (delta.isEmpty()) {
                return messages;
            }

            log.info("appId={} Tier 3 开始摘要: ratio={}%, deltaSize={}", appId,
                    String.format("%.1f", ratio * 100), delta.size());

            String newSummary = contextSummarizer.summarize(appId, lastSummary, delta);

            // 构建新消息列表：[SystemMessage(如有)] + [新摘要] + [保护区消息]
            List<ChatMessage> result = new ArrayList<>();
            if (!messages.isEmpty() && messages.getFirst() instanceof SystemMessage sys) {
                result.add(sys);
            }
            result.add(SummaryMarker.create(newSummary));
            result.addAll(messages.subList(protectionStart, messages.size()));

            store.updateMessages(appId, result);
            tokenTracker.remove(appId);

            log.info("appId={} Tier 3 摘要完成: 消息数 {} → {}", appId, messages.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("appId={} Tier 3 摘要失败，降级 PRUNE: {}", appId, e.getMessage());
            return messages;
        } finally {
            lock.set(false);
        }
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
        //delta 指的是：从上一次 token 统计快照之后，新追加到 memory 里的消息列表。
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
     * 驱逐 AiMessage 时必须同步删除其关联的 ToolExecutionResultMessage，
     * 驱逐 ToolExecutionResultMessage 时必须同步删除其对应的 AiMessage（如果 AiMessage 所有 tool result 都被删了）。
     * 否则模型会因非法消息序列报错：
     * "Messages with role 'tool' must be a response to a preceding message with 'tool_calls'"
     */
    private void evictIfNeeded(List<ChatMessage> messages) {
        while (messages.size() > MAX_STORED_MESSAGES) {
            // 找到第一个可删除的位置（跳过 SystemMessage）
            int removeIdx = (messages.getFirst() instanceof SystemMessage) ? 1 : 0;
            if (removeIdx >= messages.size()) break;

            ChatMessage toRemove = messages.get(removeIdx);

            if (toRemove instanceof AiMessage ai && ai.hasToolExecutionRequests()) {
                // 删除 AiMessage 时，必须一并删除它后面关联的所有 ToolExecutionResultMessage
                Set<String> callIds = ai.toolExecutionRequests().stream()
                        .map(r -> r.id())
                        .collect(Collectors.toSet());
                messages.remove(removeIdx);
                messages.removeIf(m -> m instanceof ToolExecutionResultMessage res && callIds.contains(res.id()));
            } else if (toRemove instanceof ToolExecutionResultMessage) {
                // 孤立的 ToolExecutionResultMessage（其 AiMessage 已被之前的驱逐删掉），直接移除
                messages.remove(removeIdx);
            } else {
                messages.remove(removeIdx);
            }
        }
    }


}
