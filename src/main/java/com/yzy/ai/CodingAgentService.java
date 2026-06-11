package com.yzy.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface CodingAgentService {
    @SystemMessage(fromResource = "prompt/coding-agent-prompt.md")
    TokenStream chat(@MemoryId Long appId, @UserMessage String userMsg);
}
