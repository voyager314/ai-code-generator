package com.yzy.ai;

import com.yzy.ai.WorkFlow.model.ImageCollectionPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ImageCollectionService {
    @SystemMessage(fromResource = "prompt/img-collect-sys-prompt.md")
    String collectImages(@UserMessage String msg);

    @SystemMessage(fromResource = "prompt/img-collect-plan-prompt.md")
    ImageCollectionPlan planImageCollection(@UserMessage String msg);
}
