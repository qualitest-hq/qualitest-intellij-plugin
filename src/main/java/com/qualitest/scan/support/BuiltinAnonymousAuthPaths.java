package com.qualitest.scan.support;

import java.util.List;
import java.util.Locale;

/**
 * 与质衡服务端 {@code ProjectAuthConfigSupport.BUILTIN_ANONYMOUS_AUTH_PATH_EXACT} 对齐的免登 path 启发式。
 * 插件无法依赖服务端模块，故在此维护同名清单；变更时请两边同步。
 */
public final class BuiltinAnonymousAuthPaths {

    public static final List<String> EXACT = List.of(
            "/login",
            "/register",
            "/captchaImage",
            "/api/account/auth/login",
            "/api/account/auth/register");

    private BuiltinAnonymousAuthPaths() {}

    /** 规范化后精确匹配内置免登 path。 */
    public static boolean matches(String apiPath) {
        String path = normalize(apiPath);
        for (String exact : EXACT) {
            if (path.equals(normalize(exact))) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String apiPath) {
        if (apiPath == null || apiPath.isBlank()) {
            return "/";
        }
        String p = apiPath.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /** method 是否适合免登启发式（仅 POST；空白视为未知，仍允许 path 命中）。 */
    public static boolean methodAllowsHeuristic(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            return true;
        }
        return "POST".equals(httpMethod.trim().toUpperCase(Locale.ROOT));
    }
}
