package com.yzy.aop;

import com.yzy.annotation.RateLimit;
import com.yzy.entity.User;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserService userService;

    @Before("@annotation(rateLimit)")
    public void func(JoinPoint joinPoint, RateLimit rateLimit) {
        String key=genKey(joinPoint,rateLimit);
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL,rateLimit.rate(), Duration.ofSeconds(rateLimit.rateInterval()),Duration.ofHours(1));
        if(!limiter.tryAcquire())throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,rateLimit.message());
    }

    private String getClientIP(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null){return "unknown";}
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if(ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip=request.getRemoteAddr();
        }
        //处理多级代理的情况
        if(ip!=null&&ip.contains(",")){
            ip=ip.split(",")[0].trim();
        }
        return ip!=null?ip:"unknown";
    }

    /**
     * 根据客户端ip/API方法名/用户id获取ratelimit key
     * @param point
     * @param rateLimit
     * @return
     */
    private String genKey(JoinPoint point,RateLimit rateLimit){
        StringBuilder key = new StringBuilder();
        if(!rateLimit.key().isEmpty())key.append(rateLimit.key()).append(":");
        switch (rateLimit.limitType()){
            case IP -> key.append(getClientIP());
            case USER -> {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    Object state = request.getSession().getAttribute("USER_LOGIN_STATE");
                    key.append(((User)state).getId());
                }else key.append(getClientIP());
            }
            case API -> {
                Signature signature = point.getSignature();
                key.append(signature.getDeclaringTypeName()).append(".").append(signature.getName());
            }
        }
        return key.toString();
    }
}
