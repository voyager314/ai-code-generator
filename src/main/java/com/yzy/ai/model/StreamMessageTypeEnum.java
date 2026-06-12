package com.yzy.ai.model;

import lombok.Getter;

/**
 * 流式消息类型枚举
 */
@Getter
public enum StreamMessageTypeEnum {

    AI_RESPONSE("ai_response", "AI响应"),
    TOOL_REQUEST("tool_request", "工具请求"),
    TOOL_EXECUTED("tool_executed", "工具执行结果"),
    REFLECTION_STARTED("reflection_started", "反思检查开始"),
    REFLECTION_RESULT("reflection_result", "反思检查结果"),
    REFLECTION_RETRY("reflection_retry", "反思修复重试"),
    APPROVAL_REQUEST("approval_request", "审批请求"),
    APPROVAL_RESULT("approval_result", "审批结果"),
    AGENT_COMPLETE("agent_complete", "Agent完成"),
    AGENT_ERROR("agent_error", "Agent错误");

    private final String value;
    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}

