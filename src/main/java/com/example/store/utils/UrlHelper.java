package com.example.store.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * URL 处理工具类
 * 用于统一给相对路径的图片加上完整的 http://localhost:端口/项目名 前缀
 */
@Component
public class UrlHelper {

    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    public String enrichUrl(String uri) {

        if (uri == null || uri.isEmpty()) {
            return null;
        }
        // 如果本来就是 http 开头就不处理
        if (uri.startsWith("http")) {
            return uri;
        }
        // 拼接前缀
        return "http://localhost:" + port + contextPath + uri;
    }
}