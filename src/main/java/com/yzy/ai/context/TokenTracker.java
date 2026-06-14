package com.yzy.ai.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 混合 token 计数追踪器。
 * <p>
 * 核心策略：API 精确基准 + 增量估算。
 * <ul>
 *   <li>每次 LLM 响应后，记录 API 返回的精确 inputTokenCount 和当时的消息条数</li>
 *   <li>下次调用前，用 "精确基准 + 新增消息的字符估算" 得到当前 token 量</li>
 *   <li>新增部分通常只有几条消息，估算误差极小</li>
 * </ul>
 */
@Slf4j
@Component
public class TokenTracker {

    private final ConcurrentHashMap<Long, Snapshot> snapshots = new ConcurrentHashMap<>();

    /**
     * LLM 响应后调用：记录本次请求的精确 inputTokenCount 和当时消息条数。
     * 同时触发 TokenEstimator 的比率校准。
     *
     * @param appId          应用 ID
     * @param inputTokens    API 返回的 input token 数
     * @param requestCharCount 本次请求消息的总字符数（用于校准）
     * @param messageCount   本次请求包含的消息条数
     */
    public void recordApiUsage(long appId, long inputTokens, int requestCharCount, int messageCount) {
        snapshots.put(appId, new Snapshot(inputTokens, messageCount));
        TokenEstimator.calibrate(inputTokens, requestCharCount);
        log.debug("TokenTracker: appId={}, inputTokens={}, msgCount={}, charsPerToken={}",
                appId, inputTokens, messageCount, String.format("%.2f", TokenEstimator.getCharsPerToken()));
    }

    /**
     * 获取上次 API 快照（精确基准）
     *
     * @return 快照，如果无历史数据则返回 null
     */
    public Snapshot getSnapshot(long appId) {
        return snapshots.get(appId);
    }

    public void remove(long appId) {
        snapshots.remove(appId);
    }

    public record Snapshot(long inputTokens, int messageCount) {}
}
