package com.yzy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 应用添加请求
 *
 * @author yzy
 * @since 2026-03-12
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用名称
     */
    @Size(max = 256, message = "应用名称长度不能超过 256 个字符")
    private String appName;

    /**
     * 应用封面
     */
    @Size(max = 512, message = "应用封面长度不能超过 512 个字符")
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    @NotNull(message = "初始化提示不能为空")
    @Size(max = 2000, message = "初始化提示长度不能超过 2000 个字符")
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    @Size(max = 64, message = "代码生成类型长度不能超过 64 个字符")
    private String codeGenType;

    @Serial
    private static final long serialVersionUID = 1L;

}