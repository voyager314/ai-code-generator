package com.yzy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话历史添加请求
 *
 * @author yzy
 * @since 2026-03-14
 */
@Data
public class ChatHistoryAddRequest implements Serializable {

    /**
     * 应用id
     */
    @NotNull(message = "应用id不能为空")
    private Long appId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容长度不能超过 10000 个字符")
    private String message;

    /**
     * 消息类型（user/ai）
     */
    @NotBlank(message = "消息类型不能为空")
    @Size(max = 32, message = "消息类型长度不能超过 32 个字符")
    private String messageType;

    @Serial
    private static final long serialVersionUID = 1L;

}
