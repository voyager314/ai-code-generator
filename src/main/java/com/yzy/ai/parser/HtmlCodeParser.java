package com.yzy.ai.parser;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yzy.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parse(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        String htmlCode = null;

        // 1. 首先尝试解析 JSON 格式（LangChain4j 结构化输出返回的是 JSON）
        try {
            JSONObject jsonObject = JSONUtil.parseObj(codeContent.trim());
            if (jsonObject.containsKey("htmlCode")) {
                htmlCode = jsonObject.getStr("htmlCode");
                if (jsonObject.containsKey("description")) {
                    result.setDescription(jsonObject.getStr("description"));
                }
            }
        } catch (Exception e) {
            // 不是 JSON 格式，继续尝试其他方式
        }

        // 2. 如果 JSON 解析失败或没有 htmlCode 字段，尝试提取代码块
        if (htmlCode == null || htmlCode.trim().isEmpty()) {
            htmlCode = extractHtmlCode(codeContent);
        }

        // 3. 设置解析结果
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果所有方式都失败，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
