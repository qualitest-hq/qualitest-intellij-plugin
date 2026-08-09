package com.qualitest;

/**
 * 插件级常量（默认值与契约字符串）。
 */
public final class QualiTestConstants {

    /** JavaDoc / Swagger 分组占位格式的默认串（含 `{group}` 占位符）。 */
    public static final String DEFAULT_GROUP_TAG = "api.group {group}";

    /**
     * 默认免登录注解短名。
     * 扫描时方法或类上出现这些注解，会把接口标成免登录；用户可在设置里增删。
     */
    public static final java.util.List<String> DEFAULT_ANONYMOUS_ANNOTATIONS = java.util.List.of("Anonymous");

    private QualiTestConstants() {}
}
