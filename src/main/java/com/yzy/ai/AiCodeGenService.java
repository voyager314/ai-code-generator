package com.yzy.ai;

import com.yzy.ai.model.HtmlCodeResult;
import com.yzy.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGenService {
    @SystemMessage(fromResource = "prompt/gen-html-sys-prompt.md")
    public HtmlCodeResult genHTMLCode(String userMsg);

    @SystemMessage(fromResource = "prompt/gen-multifile-code-sys-prompt.md")
    public MultiFileCodeResult genMultiFileCode(String userMsg);

    @SystemMessage(fromResource = "prompt/gen-html-sys-prompt.md")
    public Flux<String> genHtmlCodeStream(String userMsg);

    @SystemMessage(fromResource = "prompt/gen-multifile-code-sys-prompt.md")
    public Flux<String> genMultiFileCodeStream(String userMsg);

    @SystemMessage(fromResource = "prompt/vue-project-prompt.md")
    public Flux<String> genVueProjectStream(@MemoryId Long appId, @UserMessage String userMsg);

    //使用TokenStream获取工具调用事件
    @SystemMessage(fromResource = "prompt/vue-project-prompt.md")
    public TokenStream genVueProjectTokenStream(@MemoryId Long appId, @UserMessage String userMsg);
}
