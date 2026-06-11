package com.yzy.ai.handler;

import cn.hutool.json.JSONUtil;
import com.yzy.ai.model.*;
import com.yzy.entity.User;
import com.yzy.enums.MessageTypeEnum;
import com.yzy.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent 模式流式事件处理器
 * <p>
 * 将 TokenStream 产出的原始 JSON chunks（AiResponseMessage / ToolRequestMessage / ToolExecutedMessage）
 * 转换为前端可渲染的 AgentEvent JSON。
 * <p>
 * 核心功能：
 * - 工具名中文翻译（TOOL_NAME_MAP）：writeFile → "写入文件"，面向非程序员用户
 * - 参数摘要提取（summarizeArgs）：从工具参数 JSON 中提取关键字段（如文件路径、命令内容）
 * - 结果截断（truncateResult）：防止长输出消耗过多 token
 * - AI 文本聚合：收集完整的 AI 回复用于对话历史持久化
 * - 工具去重：同一工具调用只推送一次 TOOL_REQUEST 事件（通过 seenToolIds 跟踪）
 */
@Slf4j
@Component
public class AgentStreamHandler {

    private static final Map<String, String> TOOL_NAME_MAP = Map.of(
            "writeFile", "写入文件",
            "readFile", "读取文件",
            "modifyFile", "修改文件",
            "deleteFile", "删除文件",
            "readDir", "浏览目录",
            "searchCode", "搜索代码",
            "executeCommand", "执行命令"
    );

    public Flux<String> handle(Flux<String> origin, Long appId, User loginUser, ChatHistoryService service) {
        StringBuilder aiText = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();

        return origin
                .map(chunk -> convertToAgentEvent(chunk, aiText, seenToolIds))
                .filter(s -> !s.isEmpty())
                .doOnComplete(() ->
                        service.saveChatHistory(appId, loginUser.getId(), aiText.toString(), MessageTypeEnum.AI.getValue()))
                .doOnError(throwable ->
                        service.saveChatHistory(appId, loginUser.getId(), throwable.getMessage(), MessageTypeEnum.AI.getValue()));
    }

    private String convertToAgentEvent(String chunk, StringBuilder aiText, Set<String> seenToolIds) {
        StreamMessage msg = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum msgType = StreamMessageTypeEnum.getEnumByValue(msg.getType());
        if (msgType == null) return "";

        return switch (msgType) {
            case AI_RESPONSE -> {
                AiResponseMessage message = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = message.getData();
                aiText.append(data);
                yield JSONUtil.toJsonStr(AgentEvent.aiResponse(data));
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage message = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = message.getId();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);
                    String friendlyName = TOOL_NAME_MAP.getOrDefault(message.getName(), message.getName());
                    String argsSummary = summarizeArgs(message.getName(), message.getArguments());
                    yield JSONUtil.toJsonStr(AgentEvent.toolRequest(friendlyName, argsSummary));
                }
                yield "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage message = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String friendlyName = TOOL_NAME_MAP.getOrDefault(message.getName(), message.getName());
                String argsSummary = summarizeArgs(message.getName(), message.getArguments());
                String result = truncateResult(message.getResult());

                String display = String.format("\n\n[%s] %s\n%s\n", friendlyName, argsSummary, result);
                aiText.append(display);
                yield JSONUtil.toJsonStr(AgentEvent.toolExecuted(friendlyName, argsSummary, result));
            }
        };
    }

    /**
     * 从工具参数 JSON 中提取关键信息作为摘要，用于前端展示
     */
    private String summarizeArgs(String toolName, String argsJson) {
        try {
            var json = JSONUtil.parseObj(argsJson);
            return switch (toolName) {
                case "writeFile", "readFile", "modifyFile", "deleteFile" ->
                        json.getStr("relativeFilePath", json.getStr("relativePath", ""));
                case "readDir" -> json.getStr("relativePath", ".");
                case "searchCode" -> json.getStr("pattern", "");
                case "executeCommand" -> json.getStr("command", "");
                default -> argsJson.length() > 80 ? argsJson.substring(0, 80) + "..." : argsJson;
            };
        } catch (Exception e) {
            return argsJson != null && argsJson.length() > 80 ? argsJson.substring(0, 80) + "..." : String.valueOf(argsJson);
        }
    }

    private String truncateResult(String result) {
        if (result == null) return "";
        return result.length() > 500 ? result.substring(0, 500) + "\n...[结果已截断]" : result;
    }
}
