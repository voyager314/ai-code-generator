package com.yzy.aop;

import com.yzy.annotation.Auth;
import com.yzy.common.UserRoleEnum;
import com.yzy.entity.User;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthCheck {
    @Around("@annotation(auth)")
    public Object check(ProceedingJoinPoint joinPoint,Auth auth) throws Throwable {
        String role = auth.role();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        Object loginUser = request.getSession().getAttribute("USER_LOGIN_STATE");
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = (User) loginUser;
        if(user.getUserRole().equals(UserRoleEnum.ADMIN.getValue())){
            //管理员可以直接访问所有服务
            return joinPoint.proceed();
        }
        if(!role.equals(UserRoleEnum.getEnumByValue(user.getUserRole()).getValue()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        return joinPoint.proceed();
    }
}
