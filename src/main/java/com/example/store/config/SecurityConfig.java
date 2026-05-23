package com.example.store.config;

import com.alibaba.fastjson2.JSON;
import com.example.store.security.JwtAuthenticationFilter;
import com.example.store.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 核心配置
 *
 * 主要职责：
 * 1. 关闭 CSRF（前后端分离项目不需要）
 * 2. 设置无状态 Session（使用 JWT，不依赖 HttpSession）
 * 3. 配置公开路由白名单（无需登录可访问）
 * 4. 注册 JWT 过滤器
 * 5. 配置认证失败/权限不足的统一响应
 *
 * 注意：CORS 配置保留在 WebMvcConfig 中，Security 层不重复配置。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 开启 @PreAuthorize 注解支持
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 公开路由白名单（无需 token） */
    private static final String[] PUBLIC_URLS = {
            "/user/login",
            "/user/register",
            "/user/captcha",
            "/goods/**",
            "/shop/list",
            "/shop/detail/**",
            "/category/**",
            "/image/**",
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/api/alipay/notify",
            "/api/alipay/query"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 关闭 CSRF（前后端分离不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 2. 让 Security 使用我们定义的 CORS 配置（必须显式开启，否则 OPTIONS 预检会被拦截）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. 无状态 Session（JWT 不依赖 Session）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. 路由权限规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()   // 白名单放行
                .anyRequest().authenticated()               // 其余全部需要登录
            )

            // 5. 认证失败处理（token 无效/未登录访问受保护接口）
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    Result<Void> result = new Result<>();
                    result.againLogin("请先登录");
                    response.getWriter().write(JSON.toJSONString(result));
                })
                // 权限不足处理（已登录但角色不够）
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    Result<Void> result = new Result<>();
                    result.unauthorized("权限不足");
                    response.getWriter().write(JSON.toJSONString(result));
                })
            )

            // 6. 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 配置源
     * Security 层的 CORS 处理在过滤器链最前面，能正确响应 OPTIONS 预检请求，
     * 避免前端跨域报错。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 密码编码器
     *
     * 项目使用自定义 MD5+Salt 加密，密码验证在 UserServiceImpl 中手动完成，
     * 不走 Security 的 authenticate() 流程，所以这里用 NoOpPasswordEncoder 占位。
     *
     * 如果未来迁移到 BCrypt，把这里换成 BCryptPasswordEncoder 即可。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * 暴露 AuthenticationManager Bean
     * 供需要手动触发认证的场景使用（当前项目暂不需要，预留扩展）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
