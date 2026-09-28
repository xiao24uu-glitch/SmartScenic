package com.scenic.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    String module() default "";
    String action() default "";
    String description() default "";
}
