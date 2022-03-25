package com.example.emos.wx.config.shiro;


import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
public class OAuth2Filter extends AuthenticatingFilter {
    // 保存
    @Autowired
    private ThreadLocalToken threadLocalToken;
    // 值注入
    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;
    // jwt相关方法使用,校验令牌有效性
    @Autowired
    private JwtUtil jwtUtil;
    // RedisTemplate使用redis的读写
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        // 获取请求的token，强制转换成Http的请求
        String token = getRequestToken((HttpServletRequest) request);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return new OAuth2Token(token);
    }
    // 判断哪一个请求要被shiro处理
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        // Ajax提交application/json数据的时候，会先发出Options请求
        // 这里要放行Options请求，不需要Shiro处理
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return true;
        }
        return false;
    }

    // 对请求进行处理
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 强制转换成http的请求和响应
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 把响应的字符集设置一下
        resp.setHeader("Content-Type", "text/html;charset=UTF-8");
        //允许跨域请求
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        // 清除threadLocalToken中的数据，一旦判定要刷新，媒介类和redis的token都要刷新
        threadLocalToken.clear();
        //获取请求token，如果token不存在，直接返回401
        String token = getRequestToken((HttpServletRequest) request);
        if (StrUtil.isBlank(token)) {
            // 设置状态码，没有权限的状态码
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            return false;
        }
        try {
            // 验证token是否过期，是否有效
            jwtUtil.verifierToken(token);
        } catch (TokenExpiredException e) {
            //客户端令牌过期，查询Redis中是否存在令牌，如果存在令牌就重新生成一个令牌给客户端
            if (redisTemplate.hasKey(token)) {
                redisTemplate.delete(token);//删除旧的令牌
                int userId = jwtUtil.getUserId(token);//根据老的token获取userId
                token = jwtUtil.createToken(userId); //生成新的令牌
                //把新的令牌保存到Redis中
                redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
                //把新令牌绑定到线程（媒介类中）
                threadLocalToken.setToken(token);
            } else {
                //如果Redis不存在令牌，让用户重新登录
                resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
                resp.getWriter().print("令牌已经过期");
                return false;
            }
        } catch (Exception e) {
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            return false;
        }
        // 经过检测，发生令牌没有问题。执行realm类，executeLogin间接调用shiro的realm类
        boolean bool = executeLogin(request, response);
        return bool;
    }
    // 对认证失败进行处理
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=utf-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        try {
            resp.getWriter().print(e.getMessage());
        } catch (IOException exception) {

        }
        // 认证失败，返回false
        return false;
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        super.doFilterInternal(request, response, chain);
    }

    /**
     * 获取请求头里面的token
     */
    private String getRequestToken(HttpServletRequest httpRequest) {
        // 从header中获取token
        String token = httpRequest.getHeader("token");
        if (StrUtil.isBlank(token)) {
            // 如果是空，尝试从请求体查询token
            token = httpRequest.getParameter("token");
        }
        return token;
    }
}
