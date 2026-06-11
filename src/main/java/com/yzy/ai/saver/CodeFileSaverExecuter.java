package com.yzy.ai.saver;

import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.ai.model.HtmlCodeResult;
import com.yzy.ai.model.MultiFileCodeResult;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;

import java.io.File;

/**
 * 代码保存执行器
 */
public class CodeFileSaverExecuter {
    private static final HtmlCodeSaver htmlCodeSaver = new HtmlCodeSaver();
    private static final MultiFileCodeSaver multiFileCodeSaver = new MultiFileCodeSaver();

    public static File executeCodeSave(CodeGenTypeEnum typeEnum,Object result,Long appId){
        return switch (typeEnum){
            case HTML, DEFAULT -> htmlCodeSaver.saveCode((HtmlCodeResult) result,appId);
            case MULTI_FILE -> multiFileCodeSaver.saveCode((MultiFileCodeResult) result,appId);
            default -> throw new BusinessException(ErrorCode.OPERATION_ERROR);
        };
    }
}
