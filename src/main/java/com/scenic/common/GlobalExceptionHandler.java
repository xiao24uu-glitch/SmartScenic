package com.scenic.common;

import com.scenic.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /** 缺失静态资源（图片等）只记 DEBUG，不污染日志 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getMessage());
        return Result.error(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}
