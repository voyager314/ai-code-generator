package com.yzy.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yzy.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话历史 映射层。
 *
 * @author yzy
 * @since 2026-03-14
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

}
