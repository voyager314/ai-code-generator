package com.yzy.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzy.common.UserRoleEnum;
import com.yzy.dto.UserQueryRequest;
import com.yzy.entity.User;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.mapper.UserMapper;
import com.yzy.service.UserService;
import com.yzy.vo.UserLoginVO;
import com.yzy.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * 用户 服务层实现。
 *
 * @author yzy
 * @since 2026-03-10
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空！");
        }
        if (!checkPassword.equals(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码校验失败！");
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long cnt = mapper.selectCountByQuery(queryWrapper);
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复！");
        }
        User user = new User();
        user.setUserAccount(userAccount);
        //密码加密
        user.setUserPassword(getEncryptPassword(userPassword));
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserName("<UNK>");
        boolean saved = save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败！");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String password) {
        //盐值，混淆密码
        final String salt = "yzy";
        return DigestUtils.md5DigestAsHex((salt + password).getBytes());
    }

    @Override
    public UserLoginVO getUserLoginVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空！");
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user, userLoginVO);
        return userLoginVO;
    }

    @Override
    public UserLoginVO userLogin(String account, String password, HttpServletRequest request) {
        if (StrUtil.hasBlank(account, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper
                .eq("userAccount", account)
                .eq("userPassword", getEncryptPassword(password));
        User user = mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号或密码错误！");
        }
        request.getSession().setAttribute("USER_LOGIN_STATE", user);
        return getUserLoginVO(user);
    }

    @Override
    public boolean userLogOut(HttpServletRequest request) {
        if (request.getSession().getAttribute("USER_LOGIN_STATE") == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录！");
        }
        request.getSession().removeAttribute("USER_LOGIN_STATE");
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空！");
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (userList == null || userList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "列表为空！");
        }
        return userList
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserVO.class))
                .toList();
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        return new QueryWrapper()
                .eq("userAccount", userQueryRequest.getUserAccount())
                .eq("userRole", userQueryRequest.getUserRole())
                .eq("id", userQueryRequest.getId())
                .like("userName", userQueryRequest.getUserName())
                .like("userProfile", userQueryRequest.getUserProfile())
                .orderBy(userQueryRequest.getSortField(),userQueryRequest.getSortOrder().equals("ascend"));
    }
}
