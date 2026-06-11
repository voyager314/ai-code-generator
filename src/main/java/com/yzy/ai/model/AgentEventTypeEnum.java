package com.yzy.ai.model;

import lombok.Getter;

@Getter
public enum AgentEventTypeEnum {

    AI_RESPONSE("ai_response", "AI回复"),
    TOOL_REQUEST("tool_request", "工具调用请求"),
    TOOL_EXECUTED("tool_executed", "工具执行结果"),
    APPROVAL_REQUEST("approval_request", "审批请求"),
    APPROVAL_RESULT("approval_result", "审批结果"),
    AGENT_COMPLETE("agent_complete", "Agent完成"),
    AGENT_ERROR("agent_error", "Agent错误");

    private final String value;
    private final String text;

    AgentEventTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
