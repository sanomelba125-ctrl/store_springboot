package com.example.store.config;

import com.example.store.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // 引入 CORS 类
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    private static final String LOCAL_FILE_PATH = "file:D:/1A/store_imgs/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/image/**")
                .addResourceLocations(LOCAL_FILE_PATH);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                   // 允许所有路径
                .allowedOriginPatterns("*")          // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的方法
                .allowedHeaders("*")                 // 允许所有 Header
                .allowCredentials(true)              // 允许携带 Cookie/凭证
                .maxAge(3600);                       // 预检请求缓存时间
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                //白名单
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/captcha",
                        "/goods/**",
                        "/image/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-ui.html" ,
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/image/**",
                        "/api/alipay/notify",
                        "/api/alipay/query"
                );
    }
}