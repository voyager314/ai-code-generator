package com.yzy.ai.approval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class ApprovalService {
    private final Map<String, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();
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
