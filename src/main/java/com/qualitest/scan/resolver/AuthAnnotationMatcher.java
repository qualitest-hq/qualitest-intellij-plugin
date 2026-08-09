package com.qualitest.scan.resolver;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 免登录注解名匹配工具。
 * <p>
 * 扫描时判断 Controller/方法上的注解，是否落在用户配置的「免登录注解」名单里。
 * 配置项可以是短名（如 Anonymous）或全限定名。
 */
public final class AuthAnnotationMatcher {

    private AuthAnnotationMatcher() {}

    /**
     * 判断注解全限定名是否命中配置列表中的任一项。
     *
     * @param qualifiedName 注解全限定名，可为 null（视为未命中）
     * @param configured    用户配置的注解名列表（短名或全限定名）
     * @return 命中任一配置则 true
     */
    public static boolean matches(String qualifiedName, Collection<String> configured) {
        if (qualifiedName == null || qualifiedName.isBlank() || configured == null || configured.isEmpty()) {
            return false;
        }
        String qn = qualifiedName.trim();
        String simple = simpleName(qn);
        for (String raw : configured) {
            if (raw == null) {
                continue;
            }
            String cfg = raw.trim();
            if (cfg.isEmpty()) {
                continue;
            }
            // 全名相等，或简单名相等（区分大小写，避免 MyAnonymous 误命中 Anonymous）
            if (qn.equals(cfg) || simple.equals(cfg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从全限定名取出简单名（最后一个点号之后的部分）。
     */
    static String simpleName(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : name;
    }

    /**
     * 规范化用户填写的注解名单：
     * 按逗号、分号、换行拆分，去掉空白和空串，去重并保持顺序，保留原始大小写。
     *
     * @param raw 原始配置（可为多行文本拆成的列表，或已是单项列表）
     * @return 规范化后的不可变列表；输入为空则返回空列表
     */
    public static List<String> normalizeConfigured(Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            for (String part : item.split("[,\\n\\r;]+")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
        }
        return List.copyOf(out);
    }
}
