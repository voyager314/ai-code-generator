package com.yzy.ai.saver;

import cn.hutool.core.io.FileUtil;
import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.ai.model.MultiFileCodeResult;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class MultiFileCodeSaver extends CodeFileSaverTemplate<MultiFileCodeResult> {
    /**
     * 保存多文件代码
     *
     * @param result 模型输出结果
     * @param appId 应用id
     * @return 代码文件路径
     */
    @Override
    public File saveCode(MultiFileCodeResult result, Long appId) {
        ThrowUtil.throwIf(result == null, ErrorCode.PARAMS_ERROR);
        String htmlCode = result.getHtmlCode();
        String cssCode = result.getCssCode();
        String jsCode = result.getJsCode();
        String basePath = baseDir + File.separator + String.format("%s_%s", CodeGenTypeEnum.MULTI_FILE.getValue(), appId);

        FileUtil.mkdir(basePath);
        String htmlDir = basePath + File.separator + "index.html";
        String cssDir = basePath + File.separator + "style.css";
        String jsDir = basePath + File.separator + "script.js";
        FileUtil.writeString(htmlCode, new File(htmlDir), StandardCharsets.UTF_8);
        FileUtil.writeString(cssCode, new File(cssDir), StandardCharsets.UTF_8);
        FileUtil.writeString(jsCode, new File(jsDir), StandardCharsets.UTF_8);
        return new File(baseDir);
    }
}
