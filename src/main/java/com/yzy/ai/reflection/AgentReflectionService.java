package com.yzy.ai.reflection;

import com.yzy.ai.CodeQualityCheckService;
import com.yzy.ai.WorkFlow.model.QualityResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AgentReflectionService {

    @Autowired
    private CodeQualityCheckService codeQualityCheckService;

    @Autowired
    private CodeSnapshotReader codeSnapshotReader;

    @Autowired
    private AgentReflectionProperties properties;

    /**
     * 读取工作目录代码快照并调用质量检查。
     * 注意：checkCodeQality 是同步阻塞的 LLM 调用，
     * 调用方必须在 Schedulers.boundedElastic() 上执行此方法。
     */
    public QualityResult reflect(Long appId) {
        String codeSnapshot = codeSnapshotReader.read(appId, properties.getMaxSnapshotChars());
        if (codeSnapshot.isBlank()) {
            return QualityResult.builder()
                    .isValid(false)
                    .errors(List.of("未找到可检查代码文件"))
                    .suggestions(List.of("请先生成或写入项目代码"))
                    .build();
        }
        return codeQualityCheckService.checkCodeQality(codeSnapshot);
    }

    /**
     * 构造修复 prompt。
     * 包含用户原始需求摘要，防止 ChatMemory 窗口滑动后丢失原始上下文。
     */
    public String buildFixPrompt(QualityResult result, String originalUserMsg) {
        return """
                上一次实现已经写入工作目录，但 reflection 检查未通过。
                请读取现有文件并只修复必要问题，不要重建无关内容。

                ## 用户原始需求
                %s

                ## 必须修复的问题
                %s

                ## 修改建议
                %s
                """.formatted(
                originalUserMsg,
                String.join("\n", result.getErrors()),
                String.join("\n", result.getSuggestions())
        );
    }
}
