package com.yzy.dto;

import com.yzy.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用查询请求
 *
 * @author yzy
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    @Serial
    private static final long serialVersionUID = 1L;

}