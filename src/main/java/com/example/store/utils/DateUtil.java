package com.example.store.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间处理工具类
 * 统一处理数据库中的字符串时间格式
 */
public class DateUtil {

    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(STANDARD_FORMAT));
    }

    /**
     * 格式化指定时间
     */
    public static String format(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern(STANDARD_FORMAT));
    }
}