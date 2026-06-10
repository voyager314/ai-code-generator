package com.yzy.ai.parser;

import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;

/**
 * 代码解析执行器
 */
public class CodeParserExecuter {
    private static final HtmlCodeParser htmlParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileParser = new MultiFileCodeParser();

    public static Object executeCodeParse(String codeContent, CodeGenTypeEnum typeEnum){
        return switch (typeEnum){
            case HTML -> htmlParser.parse(codeContent);
            case MULTI_FILE -> multiFileParser.parse(codeContent);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"不支持的类型");
        };
    }
}
