package com.yzy.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 应用更新请求
 *
 * @author yzy
 * @since 2026-03-12
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * 应用id
     */
    private Long id;

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
     * 优先级
     */
    private Integer priority;

    @Serial
    private static final long serialVersionUID = 1L;

}