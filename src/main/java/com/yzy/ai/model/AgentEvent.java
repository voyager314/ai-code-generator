package com.yzy.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 模式统一事件包装
 * <p>
 * 所有推送给前端的 Agent 事件都序列化为此对象的 JSON。
 * 通过静态工厂方法构造各类事件，保证 type 字段与 AgentEventTypeEnum 一致。
 * <p>
 * 字段说明：
 * - type: 事件类型（必填）
 * - toolName: 工具名称（仅工具相关事件）
 * - toolArgs: 工具参数摘要（仅工具相关事件）
 * - content: 事件内容（AI回复文本 / 工具执行结果 / 审批描述 / 错误信息）
 * - approvalId: 审批标识（仅 HITL 事件，前端用此 ID 调用 /app/agent/approve 回传结果）
 */
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

    public static AgentEvent reflectionStarted() {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.REFLECTION_STARTED.getValue())
                .build();
    }

    public static AgentEvent reflectionResult(boolean isValid, String content) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.REFLECTION_RESULT.getValue())
                .content(content)
                .build();
    }

    public static AgentEvent reflectionRetry(int round, int maxRetries) {
        return AgentEvent.builder()
                .type(AgentEventTypeEnum.REFLECTION_RETRY.getValue())
                .content("第 " + round + "/" + maxRetries + " 轮修复")
                .build();
    }
}
