package com.example.emos.wx.config.shiro;

import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OAuth2Realm extends AuthorizingRealm {
    // 通过Autowired注解注入JwtUtil对象，要处理令牌字符串
    @Autowired
    private JwtUtil jwtUtil;
    // 做认证的时候，传入封装好的令牌对象。看是否符合OAuth2Token类型
    // 传入的是接口子类的对象AuthenticationToken token
    @Autowired
    private UserService userService;
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    /**
     * 授权（验证权限时调用）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {
        TbUser user = (TbUser) collection.getPrimaryPrincipal();
        int userId = user.getId();
        Set<String> permsSet = userService.searchUserPermissions(userId);
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        // TODO 查询用户的权限列表
        // TODO 把权限列表用户添加info对象中
        return info;
    }

    /**
     * 认证（登陆时调用）
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getPrincipal();
        int userId = jwtUtil.getUserId(accessToken);
        TbUser user = userService.searchById(userId);
        if (user == null) {
            throw new LockedAccountException("账号已经被锁定，请联系管理员");
        }
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName());
        // TODO 往info对象中添加用户信息、Token字符串
        return info;
    }
}
