package com.yzy.exception;

import cn.hutool.json.JSONUtil;
import com.yzy.common.BaseResponse;
import com.yzy.common.ResultUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        if(handleSseError(e.getCode(),e.getMessage())){return null;}
        //普通请求直接处理
        return ResultUtil.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        if(handleSseError(ErrorCode.SYSTEM_ERROR.getCode(),"系统错误")){return null;}
        return ResultUtil.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 处理SSE请求的错误响应
     * @param errorCode 错误码
     * @param msg 错误信息
     * @return 不是SSE请求false，是SSE请求且已处理true
     */
    private boolean handleSseError(int errorCode,String msg){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes==null)return false;
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        String header = request.getHeader("Accept");
        String requestURI = request.getRequestURI();
        //判断是否为SSE请求
        if(header!=null&&header.contains("text/event-stream")||requestURI.contains("/chat/gen/code")){
            try {
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection","keep-alive");
                //异常也作为SSE返回给前端
                Map<String, ? extends Serializable> data = Map.of("error", true, "errorCode", errorCode, "msg", msg);
                String sseData="event:business-error\ndata:"+ JSONUtil.toJsonStr(data)+"\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush();
                //发送结束事件
                response.getWriter().write("event:done\ndata:{}\n\n");
                response.getWriter().flush();
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
/*
注意！由于本项目使用的 Spring Boot 版本 >= 3.4、并且是 OpenAPI 3 版本的 Knife4j，
这会导致 @RestControllerAdvice 注解不兼容，所以必须给这个类加上 @Hidden 注解，
不被 Swagger 加载。虽然网上也有其他的解决方案，但这种方法是最直接有效的。
 */

