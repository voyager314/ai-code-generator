package com.yzy.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yzy.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 映射层。
 *
 * @author yzy
 * @since 2026-03-10
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
