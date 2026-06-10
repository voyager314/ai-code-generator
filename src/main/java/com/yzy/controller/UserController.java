package com.yzy.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yzy.annotation.Auth;
import com.yzy.common.BaseResponse;
import com.yzy.common.ResultUtil;
import com.yzy.dto.*;
import com.yzy.entity.User;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.service.UserService;
import com.yzy.vo.UserLoginVO;
import com.yzy.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author yzy
 * @since 2026-03-10
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * @param request 请求参数
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest request){
        ThrowUtil.throwIf(request==null, ErrorCode.PARAMS_ERROR);
        long id = userService.userRegister(request.getUserAccount(), request.getUserPassword(), request.getCheckPassword());
        return ResultUtil.success(id);
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @param httpServletRequest session
     * @return 脱敏用户对象封装
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> login(@RequestBody UserLoginRequest request, HttpServletRequest httpServletRequest){
        ThrowUtil.throwIf(request==null, ErrorCode.PARAMS_ERROR);
        UserLoginVO userLoginVO = userService.userLogin(request.getUserAccount(), request.getUserPassword(), httpServletRequest);
        return ResultUtil.success(userLoginVO);
    }

    /**
     * 获取已登录用户信息
     * @param request session
     * @return 脱敏用户对象封装
     */
    @GetMapping("/get/login")
    public BaseResponse<UserLoginVO> getLoginUser(HttpServletRequest request){
        Object currUser = request.getSession().getAttribute("USER_LOGIN_STATE");
        if(currUser==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserLoginVO userLoginVO = userService.getUserLoginVO((User) currUser);
        return ResultUtil.success(userLoginVO);
    }

    /**
     * 用户登出
     * @param request session
     * @return 是否登出成功
     *
     */
    @DeleteMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){
        return ResultUtil.success(userService.userLogOut(request));
    }


    /**
     * 根据主键删除用户。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Auth(role = "admin")
    public boolean remove(@PathVariable Long id) {
        return userService.removeById(id);
    }


    /**
     * 查询所有用户。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Auth(role = "admin")
    public List<User> list() {
        return userService.list();
    }

    /**
     * 根据主键获取用户。
     *
     * @param id 用户主键
     * @return 用户详情
     */
    @GetMapping("getInfo/{id}")
    @Auth(role = "admin")
    public User getInfo(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 分页查询用户。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    @Auth(role = "admin")
    public Page<User> page(Page<User> page) {
        return userService.page(page);
    }

    @PostMapping("/add")
    @Auth(role = "admin")
    public BaseResponse<Long> add(@RequestBody UserAddRequest addRequest){
        ThrowUtil.throwIf(addRequest==null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        user.setUserAccount(addRequest.getUserAccount());
        final String DEFAULT_PASSWORD = "123456";
        user.setUserPassword(userService.getEncryptPassword(DEFAULT_PASSWORD));
        ThrowUtil.throwIf(userService.save(user), ErrorCode.PARAMS_ERROR);
        return ResultUtil.success(user.getId());
    }

    @PostMapping("/update")
    @Auth(role = "admin")
    public BaseResponse<Boolean> update(@RequestBody UserUpdateRequest updateRequest){
        ThrowUtil.throwIf(updateRequest==null||updateRequest.getId()==null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(updateRequest,user);
        boolean updated = userService.updateById(user);
        return ResultUtil.success(updated);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        User user = userService.getById(id);
        ThrowUtil.throwIf(user==null, ErrorCode.NOT_FOUND_ERROR);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return ResultUtil.success(userVO);
    }

    @GetMapping("/list/page/vo")
    @Auth(role = "admin")
    public BaseResponse<Page<UserVO>> userVOPage(@RequestBody UserQueryRequest request){
        ThrowUtil.throwIf(request==null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = userService.getQueryWrapper(request);
        int pageSize = request.getPageSize();
        int pageNum = request.getPageNum();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), queryWrapper);
        //数据脱敏处理
        Page<UserVO> userVOPage=new Page<>(pageNum,pageSize,userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtil.success(userVOPage);
    }

}
