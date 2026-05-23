package com.example.store.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.store.entity.User;
import com.example.store.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户加载服务
 *
 * Security 在认证时会调用此类的 loadUserByUsername 方法，
 * 根据用户名从数据库查询用户并返回 UserDetails。
 *
 * 注意：此处只用于 Security 内部认证流程，
 * 实际登录逻辑（验证码校验、互踢等）仍在 UserServiceImpl 中处理。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        return new LoginUser(user);
    }
}
