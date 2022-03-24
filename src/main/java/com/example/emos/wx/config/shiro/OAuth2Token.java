package com.example.emos.wx.config.shiro;

import org.apache.shiro.authc.AuthenticationToken;

// 扩展接口，代表是一个合格的类，可以存储
public class OAuth2Token implements AuthenticationToken {
    private String token;
    // 构造器，对变量赋值
    public OAuth2Token(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal () {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

}
