package com.yzy.dto;

import lombok.Data;

@Data
public class AgentApprovalRequest {
    private String approvalId;
    private boolean approved;
}
