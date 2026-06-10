package com.yzy.ai.saver;

import cn.hutool.core.io.FileUtil;
import com.yzy.ai.model.CodeGenTypeEnum;
import com.yzy.ai.model.HtmlCodeResult;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class HtmlCodeSaver extends CodeFileSaverTemplate<HtmlCodeResult>{
    /**
     * 保存单个html文件
     * @param result 模型输出结果
     * @param appId 应用id
     * @return 代码文件路径
     */
    @Override
    public File saveCode(HtmlCodeResult result,Long appId) {
        ThrowUtil.throwIf(result==null||result.getHtmlCode()==null, ErrorCode.PARAMS_ERROR);
        String htmlCode = result.getHtmlCode();
        String codeDir=baseDir+File.separator+String.format("%s_%s", CodeGenTypeEnum.HTML.getValue(), appId);
        FileUtil.mkdir(codeDir);
        codeDir+=File.separator+"index.html";
        return FileUtil.writeString(htmlCode,new File(codeDir), StandardCharsets.UTF_8);
    }
}
