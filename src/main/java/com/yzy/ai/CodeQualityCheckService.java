package com.yzy.ai;

import com.yzy.ai.WorkFlow.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage(fromResource = "prompt/quality-check-prompt.md")
public interface CodeQualityCheckService {
    QualityResult checkCodeQality(@UserMessage String msg);
}
