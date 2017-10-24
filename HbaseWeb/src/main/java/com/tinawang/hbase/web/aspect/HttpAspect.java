package com.tinawang.hbase.web.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Created by wangting26 on 2017/10/9.
 */
@Slf4j
@Aspect
@Component
public class HttpAspect {

    @Pointcut("execution(public * com.tinawang.hbase.web.controller.HbaseWebController.*(..))")
    public void log(){}

    @Before("log()")
    public void doBefore(JoinPoint joinPoint){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        log.info("url={}", request.getRequestURL());
        log.info("method={}",request.getMethod());
        log.info("ip={}",request.getRemoteAddr());
        log.info("class_method={}",joinPoint.getSignature().getDeclaringTypeName()
                    + "." + joinPoint.getSignature().getName());
        log.info("args={}", Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(returning = "object",pointcut = "log()")
    public void doAfterReturning(Object object){
        log.info("response={}",object.toString());
    }
}
