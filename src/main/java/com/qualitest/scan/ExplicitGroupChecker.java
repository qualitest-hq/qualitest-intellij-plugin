package com.qualitest.scan;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.visitor.ControllerVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * 判断 Controller 是否带有显式分组注释（@api.group / @Tag），不含路径或类名兜底。
 */
public final class ExplicitGroupChecker {

    private final JavaDocResolver javaDocResolver;
    private final AnnotationResolver annotationResolver;
    private final ControllerVisitor controllerVisitor;

    public ExplicitGroupChecker(@NotNull String groupTag) {
        this.javaDocResolver = new JavaDocResolver(groupTag);
        this.annotationResolver = new AnnotationResolver();
        this.controllerVisitor = new ControllerVisitor();
    }

    /**
     * Controller 视为「带分组注释」当且仅当类或任意 API 方法上有分组 JavaDoc 或 @Tag。
     */
    public boolean hasExplicitGroup(@NotNull PsiClass controller) {
        if (hasExplicitGroupOnElement(controller)) {
            return true;
        }
        for (PsiMethod method : controllerVisitor.getApiMethods(controller)) {
            if (hasExplicitGroupOnElement(method)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExplicitGroupOnElement(@NotNull com.intellij.psi.PsiElement element) {
        String apiGroup = javaDocResolver.getApiGroup(element);
        if (isNonEmpty(apiGroup)) {
            return true;
        }
        if (element instanceof com.intellij.psi.PsiModifierListOwner owner) {
            String tagName = annotationResolver.getTagName(owner);
            return isNonEmpty(tagName);
        }
        return false;
    }

    private static boolean isNonEmpty(String value) {
        return value != null && !value.isBlank();
    }
}
