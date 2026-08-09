package com.qualitest.scan.resolver;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 测谁：免登录注解名匹配与配置规范化。
 * 边界：短名/全名命中、部分后缀误伤、多行逗号混写。
 * 单跑：在插件根目录执行 {@code gradlew test --tests AuthAnnotationMatcherTest}
 */
public class AuthAnnotationMatcherTest {

    /**
     * 前提：配置短名 Anonymous，注解全名为 …Anonymous。
     * 期望：命中。
     */
    @Test
    public void matchesShortNameAgainstFqcn() {
        assertTrue(AuthAnnotationMatcher.matches(
                "com.ruoyi.common.annotation.Anonymous",
                List.of("Anonymous")));
    }

    /**
     * 前提：配置完整全限定名，与注解全名相同。
     * 期望：命中。
     */
    @Test
    public void matchesExactFqcn() {
        assertTrue(AuthAnnotationMatcher.matches(
                "com.example.security.NoAuth",
                List.of("com.example.security.NoAuth")));
    }

    /**
     * 前提：配置 Anonymous，注解简单名为 MyAnonymous。
     * 期望：不命中。
     */
    @Test
    public void doesNotMatchPartialSuffix() {
        assertFalse(AuthAnnotationMatcher.matches(
                "com.example.MyAnonymous",
                List.of("Anonymous")));
    }

    /**
     * 前提：配置里混有逗号与换行。
     * 期望：拆成多项、去重、保持顺序。
     */
    @Test
    public void normalizeConfigured_splitsLinesAndCommas() {
        List<String> out = AuthAnnotationMatcher.normalizeConfigured(
                List.of("Anonymous", "PermitAll, SaIgnore\ncom.example.NoAuth"));
        assertEquals(List.of("Anonymous", "PermitAll", "SaIgnore", "com.example.NoAuth"), out);
    }
}
