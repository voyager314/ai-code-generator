package com.yzy.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
@Description("生成单文件HTML代码的结果")
@Data
public class HtmlCodeResult {

    @Description("HTML代码")//添加描述便于模型理解
    private String htmlCode;

    @Description("生成代码的描述")
    private String description;
}

