package com.yzy.dto;

import com.yzy.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史查询请求
 *
 * @author yzy
 * @since 2026-03-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 对话历史id
     */
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;

    /**
     * 创建时间起始（用于游标分页，加载更早的历史记录）
     */
    private LocalDateTime createTimeBefore;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;

}
