package com.yzy.ai.WorkFlow.state;

import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.ai.WorkFlow.model.QualityResult;
import com.yzy.ai.model.CodeGenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文 - 存储所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkFlowContext implements Serializable {

    /**
     * WorkFlowContext 在 MessagesState 中的存储key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 应用id
     */
    private Long appId=0L;

    /**
     * 代码检查结果
     */
    private QualityResult qualityResult;

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 图片资源字符串
     */
    private String imageListStr;

    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum generationType;

    /**
     * 生成的代码目录
     */
    private String generatedCodeDir;

    /**
     * 构建成功的目录
     */
    private String buildResultDir;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 工作流代码校验失败重试上限，默认5次
     */
    private Integer retryCnt=5;

    @Serial
    private static final long serialVersionUID = 1L;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkFlowContext
     */
    public static WorkFlowContext getContext(MessagesState<String> state) {
        return (WorkFlowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkFlowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkFlowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}

