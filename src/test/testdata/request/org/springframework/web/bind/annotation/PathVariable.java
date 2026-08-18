package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试用 PathVariable 注解桩，带 required 属性（默认 true）。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {
    /** 路径变量名，与 name 同义 */
    String value() default "";

    /** 路径变量名 */
    String name() default "";

    /** 是否必填，默认 true */
    boolean required() default true;
}
