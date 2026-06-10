package com.yzy.dto;

import com.yzy.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 精选应用查询请求
 *
 * @author yzy
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class AppStarQueryRequest extends PageRequest {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 优先级（只查询有优先级的应用）
     */
    private Integer priority;

}