package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;


// 在filter和AOP之间传递数据的媒介类
@Component
public class ThreadLocalToken {
    private ThreadLocal local = new ThreadLocal();
    public void setToken(String token) {
        local.set(token);
    }
    public String getToken() {
        return (String) local.get();
    }
    public void clear(){
        local.remove();
    }
}
