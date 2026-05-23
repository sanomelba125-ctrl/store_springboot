package com.example.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置
 *
 * 认证/授权逻辑已迁移到 SecurityConfig + JwtAuthenticationFilter，
 * 此处只保留静态资源映射和 CORS 配置。
 *
 * 注意：Spring Security 有自己的 CORS 处理，
 * 这里的 CORS 配置对 Security 过滤链之后的请求生效，两者互补。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String LOCAL_FILE_PATH = "file:D:/1A/store_imgs/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/image/**")
                .addResourceLocations(LOCAL_FILE_PATH);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
