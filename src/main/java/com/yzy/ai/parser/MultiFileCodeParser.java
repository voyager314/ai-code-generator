package com.yzy.ai.parser;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yzy.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parse(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        String htmlCode = null;
        String cssCode = null;
        String jsCode = null;

        // 1. 首先尝试解析 JSON 格式（LangChain4j 结构化输出返回的是 JSON）
        try {
            JSONObject jsonObject = JSONUtil.parseObj(codeContent.trim());
            if (jsonObject.containsKey("htmlCode")) {
                htmlCode = jsonObject.getStr("htmlCode");
            }
            if (jsonObject.containsKey("cssCode")) {
                cssCode = jsonObject.getStr("cssCode");
            }
            if (jsonObject.containsKey("jsCode")) {
                jsCode = jsonObject.getStr("jsCode");
            }
            if (jsonObject.containsKey("description")) {
                result.setDescription(jsonObject.getStr("description"));
            }
        } catch (Exception e) {
            // 不是 JSON 格式，继续尝试其他方式
        }

        // 2. 如果 JSON 解析失败或某些字段为空，使用代码块提取作为回退
        if (htmlCode == null || htmlCode.trim().isEmpty()) {
            htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        }
        if (cssCode == null || cssCode.trim().isEmpty()) {
            cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        }
        if (jsCode == null || jsCode.trim().isEmpty()) {
            jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        }

        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
