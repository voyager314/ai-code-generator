package com.yzy.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 通用编程 Agent 服务接口
 * <p>
 * 通过 LangChain4j AiServices 代理实现，LLM 自主决定调用哪些工具（文件读写、搜索、命令执行等），
 * 形成 plan-act-observe 迭代循环，直到任务完成。
 * 返回 TokenStream 以支持流式输出和工具调用事件的实时推送。
 */
public interface CodingAgentService {
    @SystemMessage(fromResource = "prompt/coding-agent-prompt.md")
    TokenStream chat(@MemoryId Long appId, @UserMessage String userMsg);
}
