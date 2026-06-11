package com.yzy.dto;

import lombok.Data;

/**
 * Agent HITL 审批请求 DTO
 * <p>
 * 前端收到 SSE approval_request 事件后，用户操作确认/拒绝，
 * 通过 POST /app/agent/approve 将结果回传。
 */
@Data
public class AgentApprovalRequest {
    /** 与 SSE 审批事件中的 approvalId 对应 */
    private String approvalId;
    /** true=批准执行，false=拒绝 */
    private boolean approved;
}
