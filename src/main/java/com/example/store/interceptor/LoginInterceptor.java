package com.example.store.interceptor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.store.utils.Result;
import com.example.store.utils.ResultType;
import jakarta.servlet.http.HttpServletRequest; // SpringBoot3 使用 jakarta
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_TOKEN_PREFIX = "login_token:";
    private static final String KEY_USER_PREFIX = "login_user_token:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Header 中的 Token
        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            returnNoLogin(response);
            return false;
        }

        // 查询 Redis
        String userJson = redisTemplate.opsForValue().get(KEY_TOKEN_PREFIX + token);

        if (!StringUtils.hasText(userJson)) {
            // Token 不存在或已过期或者是被踢下线了
            returnNoLogin(response);
            return false;
        }

        // 解析 User 信息
        JSONObject userObj = JSON.parseObject(userJson);
        String userId = userObj.getString("id");

        // 自动续期
        redisTemplate.expire(KEY_TOKEN_PREFIX + token, 30, TimeUnit.MINUTES);
        redisTemplate.expire(KEY_USER_PREFIX + userId, 30, TimeUnit.MINUTES);

        return true;
    }


    private void returnNoLogin(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        Result result = new Result();
        result.againLogin("登录已过期或在其他设备登录，请重新登录"); // 对应 ResultType.AGAIN_LOGIN (600)
        response.getWriter().write(JSON.toJSONString(result));
    }
}