package com.yzy.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {
    private String type;
    private String toolName;
    private String toolArgs;
    private String content;
    private String approvalId;

    public static AgentEvent aiResponse(String content) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.AI_RESPONSE.getValue())
                .content(content)
                .build();
    }

    public static AgentEvent toolRequest(String toolName, String toolArgs) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.TOOL_REQUEST.getValue())
                .toolName(toolName)
                .toolArgs(toolArgs)
                .build();
    }

    public static AgentEvent toolExecuted(String toolName, String toolArgs, String result) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.TOOL_EXECUTED.getValue())
                .toolName(toolName)
                .toolArgs(toolArgs)
                .content(result)
                .build();
    }

    public static AgentEvent approvalRequest(String approvalId, String description) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.APPROVAL_REQUEST.getValue())
                .approvalId(approvalId)
                .content(description)
                .build();
    }

    public static AgentEvent approvalResult(String approvalId, boolean approved) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.APPROVAL_RESULT.getValue())
                .approvalId(approvalId)
                .content(approved ? "approved" : "rejected")
                .build();
    }

    public static AgentEvent complete() {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.AGENT_COMPLETE.getValue())
                .build();
    }

    public static AgentEvent error(String message) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.AGENT_ERROR.getValue())
                .content(message)
                .build();
    }
}
