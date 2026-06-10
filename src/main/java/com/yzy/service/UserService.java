package com.yzy.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yzy.dto.UserQueryRequest;
import com.yzy.entity.User;
import com.yzy.vo.UserLoginVO;
import com.yzy.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author yzy
 * @since 2026-03-10
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 密码加密
     * @param password 原密码
     * @return 加密后的密码
     */
    public String getEncryptPassword(String password);

    /**
     * 获取登录用户
     * @param user 用户表数据封装实体
     * @return 脱敏用户数据
     */
    public UserLoginVO getUserLoginVO(User user);

    /**
     * 用户登录
     * @param account 账号
     * @param password 密码
     * @param request 网络请求
     * @return 脱敏用户数据
     */
    public UserLoginVO userLogin(String account, String password, HttpServletRequest request);

    /**
     * 用户登出
     * @param request session
     * @return 登出是否成功
     */
    public boolean userLogOut(HttpServletRequest request);

    /**
     * 获取用户VO
     * @param user 用户表数据对象
     * @return 用户VO
     */
    public UserVO getUserVO(User user);

    /**
     * 获取UserVO列表
     * @param userList 用户列表
     * @return VO列表
     */
    public List<UserVO> getUserVOList(List<User> userList);

    /**
     * 把查询请求转换为querywrapper，便于数据库查询
     * @param userQueryRequest 查询请求
     * @return 封装好的查询条件
     */
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
