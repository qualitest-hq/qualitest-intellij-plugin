package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试用 @PostMapping 注解桩：声明 POST 路径与 consumes。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostMapping {
    String[] value() default {};

    String[] path() default {};

    String[] consumes() default {};

    String[] produces() default {};
}
