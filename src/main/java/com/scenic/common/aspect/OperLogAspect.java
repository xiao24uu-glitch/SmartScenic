package com.scenic.common.aspect;

import com.scenic.common.annotation.OperLog;
import com.scenic.entity.SysOperLog;
import com.scenic.entity.SysUser;
import com.scenic.mapper.SysOperLogMapper;
import com.scenic.mapper.SysUserMapper;
import com.scenic.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志 AOP 切面
 * 拦截所有标注 @OperLog 的方法，自动记录到 sys_oper_log 表
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper sysOperLogMapper;
    private final SysUserMapper sysUserMapper;
    private final SecurityUtil securityUtil;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();

        SysOperLog logEntity = new SysOperLog();
        // 操作人（取真实姓名，没有则用用户名）
        try {
            Long uid = securityUtil.getCurrentUserId();
            logEntity.setUserId(uid);
            String operatorName = securityUtil.getCurrentUsername();
            if (uid != null) {
                SysUser user = sysUserMapper.selectById(uid);
                if (user != null) {
                    operatorName = user.getRealName() != null && !user.getRealName().isEmpty()
                            ? user.getRealName() : user.getUsername();
                }
            }
            logEntity.setUsername(operatorName);
        } catch (Exception ignored) {
            logEntity.setUsername("系统");
        }

        // 注解信息
        logEntity.setModule(operLog.module());
        logEntity.setAction(operLog.action());
        logEntity.setDescription(operLog.description());

        // 请求信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntity.setRequestMethod(request.getMethod());
                logEntity.setRequestUrl(request.getRequestURI());
                logEntity.setIp(getClientIp(request));
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败: {}", e.getMessage());
        }

        // 请求参数（截断至 2000 字符）
        try {
            String params = Arrays.stream(joinPoint.getArgs())
                    .map(arg -> arg instanceof String ? (String) arg : String.valueOf(arg))
                    .collect(Collectors.joining(", "));
            if (params.length() > 2000) params = params.substring(0, 2000) + "...";
            logEntity.setRequestParams(params);
        } catch (Exception ignored) {}

        Object result;
        try {
            result = joinPoint.proceed();
            logEntity.setStatus(1);
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            throw e;
        } finally {
            logEntity.setCostTime(System.currentTimeMillis() - start);
            try {
                sysOperLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("写入操作日志失败", e);
            }
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
