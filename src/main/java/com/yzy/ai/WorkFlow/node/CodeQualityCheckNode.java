package com.yzy.ai.WorkFlow.node;

import cn.hutool.core.io.FileUtil;
import com.yzy.ai.CodeQualityCheckService;
import com.yzy.ai.WorkFlow.model.QualityResult;
import com.yzy.ai.WorkFlow.state.WorkFlowContext;
import com.yzy.ai.WorkFlow.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CodeQualityCheckNode {
    private static final List<String> CODE_EXTENTIONS= Arrays.asList(".html",".htm",".css",".js",".ts","tsx",".jsx"
    ,".json",".vue");
    private static String readAndConcatCode(String codeDir){
        if(codeDir == null||codeDir.isEmpty()){
            log.error("codeDir is null or empty");
            return "";
        }
        File codeDirFile = new File(codeDir);
        if(!codeDirFile.exists()){
            log.error("codeDir file does not exist");
            return "";
        }
        if(!codeDirFile.isDirectory()){
            log.error("codeDir is not a directory");
            return "";
        }
        StringBuilder content = new StringBuilder();
        content.append("## 项目文件结构和代码\n");
        //遍历所有文件
        FileUtil.walkFiles(codeDirFile,file -> {
            if(skipFiles(codeDirFile,file)){return;}
            String lowerCase = file.getName().toLowerCase();
            if(CODE_EXTENTIONS.stream().anyMatch(lowerCase::endsWith)){
                String subPath = FileUtil.subPath(codeDir, file);
                content.append("### 文件：").append(subPath).append("\n");
                String s = FileUtil.readUtf8String(file);
                content.append(s).append("\n\n");
            }
        });
        return content.toString();
    }

    private static boolean skipFiles(File root,File file){
        String subPath = FileUtil.subPath(root.getAbsolutePath(), file);
        if(subPath.startsWith(".")){
            log.info("跳过隐藏文件");
            return true;
        }
        return subPath.contains("node_modules"+File.separator)
                ||subPath.contains("dist"+File.separator)
                ||subPath.contains(".git")
                ||subPath.contains("target");
    }

    /**
     * 代码质量检查
     * Reflection模式（反思模式）
     * @return
     */
    public static AsyncNodeAction<MessagesState<String>> create(){
        return AsyncNodeAction.node_async(state->{
            WorkFlowContext context = WorkFlowContext.getContext(state);
            String codeDir = context.getGeneratedCodeDir();
            String s = readAndConcatCode(codeDir);
            QualityResult qualityResult = null;
            if(s.isEmpty()){
                qualityResult=QualityResult.builder()
                        .isValid(false)
                        .errors(List.of("未找到可检查文件"))
                        .suggestions(List.of("请确保代码生成成功"))
                        .build();
            }else {
                //引入反思者（reflector）Agent
                CodeQualityCheckService bean = SpringContextUtil.getBean(CodeQualityCheckService.class);
                qualityResult = bean.checkCodeQality(s);
            }
            context.setCurrentStep("代码校验");
            context.setQualityResult(qualityResult);
            return WorkFlowContext.saveContext(context);
        });
    }
}
