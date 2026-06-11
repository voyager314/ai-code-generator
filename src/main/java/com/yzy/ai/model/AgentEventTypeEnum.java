package com.yzy.ai.model;

import lombok.Getter;

/**
 * Agent 模式 SSE 事件类型枚举
 * <p>
 * 前端根据 type 字段区分事件并做对应的 UI 渲染：
 * - ai_response / tool_request / tool_executed：常规 Agent 交互流
 * - approval_request / approval_result：HITL 审批流程
 * - agent_complete / agent_error：终态信号
 */
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
