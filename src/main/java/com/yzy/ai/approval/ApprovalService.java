package com.yzy.ai.approval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.*;

/**
 * HITL (Human-in-the-Loop) 审批服务
 * <p>
 * 作为 Agent 工具层与 SSE 流之间的桥梁：
 * - appSinks：appId → FluxSink 映射，工具通过 sink 向 SSE 流注入 APPROVAL_REQUEST 事件
 * - pendingApprovals：approvalId → CompletableFuture 映射，工具线程在此阻塞等待用户响应
 * <p>
 * 流程：
 * 1. processAgentTokenStream() 注册 sink → registerSink()
 * 2. CommandExecuteTool 执行前 → getSink() 推送审批事件 → requestAndWait() 阻塞
 * 3. 前端收到 SSE 审批事件 → 用户点击 → POST /app/agent/approve → submitApproval() 解除阻塞
 * 4. 会话结束 → removeSink() 清理
 */
@Slf4j
@Component
public class ApprovalService {
    /** approvalId → 等待用户响应的 Future */
    private final Map<String, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();
    /** appId → SSE 事件流的 Sink，用于从工具线程向前端注入审批事件 */
    private final Map<Long, FluxSink<String>> appSinks = new ConcurrentHashMap<>();

    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    public void registerSink(Long appId, FluxSink<String> sink) {
        appSinks.put(appId, sink);
    }

    public void removeSink(Long appId) {
        appSinks.remove(appId);
    }

    public FluxSink<String> getSink(Long appId) {
        return appSinks.get(appId);
    }

    /**
     * 创建审批 Future 并阻塞等待用户响应，超时返回 false（拒绝）
     */
    public boolean requestAndWait(String approvalId, long timeoutSeconds) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingApprovals.put(approvalId, future);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("审批超时: {}", approvalId);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            log.error("审批异常: {}", approvalId, e);
            return false;
        } finally {
            pendingApprovals.remove(approvalId);
        }
    }

    public boolean requestAndWait(String approvalId) {
        return requestAndWait(approvalId, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 前端通过 REST 回调提交审批结果，complete 对应的 Future 以解除工具线程阻塞
     */
    public boolean submitApproval(String approvalId, boolean approved) {
        CompletableFuture<Boolean> future = pendingApprovals.get(approvalId);
        if (future == null) {
            log.warn("审批ID不存在或已过期: {}", approvalId);
            return false;
        }
        future.complete(approved);
        return true;
    }
}
