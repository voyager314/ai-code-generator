package com.yzy.ai;

import com.yzy.ai.model.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

public interface AiRoutingService {
    //ai智能路由服务
    @SystemMessage(fromResource = "prompt/router-prompt.md")
    public CodeGenTypeEnum aiRoutingService(String userMsg);
}
