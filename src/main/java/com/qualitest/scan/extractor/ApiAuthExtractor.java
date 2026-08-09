package com.qualitest.scan.extractor;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.qualitest.scan.model.ApiAuthConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AuthAnnotationMatcher;

import java.util.Collection;
import java.util.List;

/**
 * 扫描鉴权信息：根据免登录注解名单，给接口打上 auth 标签。
 * <p>
 * 规则：API 方法或其所属 Controller 类上，只要命中名单中任一注解，
 * 则标记为免登录（mode=none）；否则标记为需要登录（mode=inherit）。
 */
public class ApiAuthExtractor implements ApiExtractor {

    /** 已规范化的免登录注解名列表。 */
    private final List<String> anonymousAnnotations;

    /**
     * @param anonymousAnnotations 免登录注解短名或全限定名；构造时会规范化
     */
    public ApiAuthExtractor(Collection<String> anonymousAnnotations) {
        this.anonymousAnnotations = AuthAnnotationMatcher.normalizeConfigured(anonymousAnnotations);
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        if (isAnonymous(method) || isAnonymous(controllerClass)) {
            api.setAuth(ApiAuthConfig.none());
        } else {
            api.setAuth(ApiAuthConfig.inherit());
        }
    }

    /**
     * 判断方法或类上是否带有免登录注解。
     */
    private boolean isAnonymous(PsiModifierListOwner owner) {
        if (owner == null || anonymousAnnotations.isEmpty()) {
            return false;
        }
        PsiAnnotation[] annotations = owner.getAnnotations();
        if (annotations.length == 0) {
            return false;
        }
        for (PsiAnnotation annotation : annotations) {
            if (AuthAnnotationMatcher.matches(annotation.getQualifiedName(), anonymousAnnotations)) {
                return true;
            }
        }
        return false;
    }
}
