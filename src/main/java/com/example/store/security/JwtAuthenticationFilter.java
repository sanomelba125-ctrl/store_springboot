package com.example.store.security;

import com.alibaba.fastjson2.JSON;
import com.example.store.utils.JwtUtil;
import com.example.store.utils.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（替代原 LoginInterceptor）
 *
 * 每次请求执行一次，流程：
 * 1. 从 Header 中取出 token
 * 2. 验证 JWT 签名和过期时间
 * 3. 查询 Redis 黑名单（登出/禁用的 token 在此被拦截）
 * 4. 将用户信息写入 SecurityContext，后续 Controller 可直接获取
 *
 * 无 token 或 token 无效时不报错，直接放行——
 * 是否需要登录由 SecurityConfig 的路由规则决定。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Redis 黑名单 key 前缀：jwt:blacklist:{jti} */
    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1. 验证 JWT 签名和过期时间
                String jti = jwtUtil.getJti(token);

                // 2. 查询黑名单（登出/禁用的 token）
                Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    writeUnauthorized(response);
                    return;
                }

                // 3. 解析用户信息，写入 SecurityContext
                String userId = jwtUtil.getUserId(token);
                Integer userType = jwtUtil.getUserType(token);

                String role = switch (userType) {
                    case 1 -> "ROLE_SUPER_ADMIN";
                    case 2 -> "ROLE_SHOP_ADMIN";
                    default -> "ROLE_USER";
                };

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,   // principal：userId，Controller 中用 @AuthenticationPrincipal 或 SecurityContextHolder 获取
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                // token 签名无效或已过期
                writeUnauthorized(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 token
     * 支持两种格式：
     *   - "token: xxx"（原有格式，前端无需改动）
     *   - "Authorization: Bearer xxx"（标准格式）
     */
    private String resolveToken(HttpServletRequest request) {
        // 优先读原有 token header（兼容现有前端）
        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        // 兼容标准 Bearer 格式
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK); // 保持 200，用业务状态码区分
        Result<Void> result = new Result<>();
        result.againLogin("登录已过期或在其他设备登录，请重新登录");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
