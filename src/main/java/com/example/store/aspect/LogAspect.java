package com.example.store.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Component
public class LogAspect {
    // 获取日志对象
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    //拦截所有的controller
    @Pointcut("execution(* com.example.store.controller.*.*(..))")
    public void controllerLog() {}

    @Before("controllerLog()")
    public void doBefore(JoinPoint joinPoint) {
        // 获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;
        HttpServletRequest request = attributes.getRequest();

        log.info("============== AOP日志开始 ==============");
        log.info("请求地址: {}", request.getRequestURL().toString());
        log.info("请求方式: {}", request.getMethod());
        log.info("执行方法: {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        log.info("请求参数: {}", Arrays.toString(joinPoint.getArgs()));
        log.info("============== AOP日志结束 ==============");
    }
}