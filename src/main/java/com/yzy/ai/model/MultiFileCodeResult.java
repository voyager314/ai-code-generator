package com.yzy.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("生成多文件代码的结果")
@Data
public class MultiFileCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("CSS样式")
    private String cssCode;

    @Description("javascript代码")
    private String jsCode;

    @Description("生成代码的描述")
    private String description;
}

