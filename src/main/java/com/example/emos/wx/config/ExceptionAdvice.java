package com.example.emos.wx.config;


import com.example.emos.wx.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 日志模块
@Slf4j
// 捕获spring mvc 抛出的异常
@RestControllerAdvice
public class ExceptionAdvice {
    // 错误消息写到响应里面的
    @ResponseBody
    // 确定状态码：500
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    // 加上这个注解，对全局进行捕获异常
    @ExceptionHandler(Exception.class)
    public String validExceptionHandler(Exception e) {
        log.error("执行异常", e);
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException exception = (MethodArgumentNotValidException) e;
            // 调用各种方法获取异常消息
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        else if (e instanceof EmosException) {
            EmosException emosException = (EmosException) e;
            // 取出异常消息
            return emosException.getMsg();
        }
        else if (e instanceof UnauthenticatedException) {
            // 未授权的类型
            return "你不具有相关权限";
        }
        else {
            // 普通的java异常
            return "后端执行异常";
        }
    }
}
