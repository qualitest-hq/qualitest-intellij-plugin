package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * API状态提取器
 * 按优先级提取API状态：
 * 1. @Deprecated 注解
 * 2. @Operation.deprecated
 * 3. JavaDoc @deprecated 标签
 * 默认: normal
 *
 * @author qualitest
 */
public class ApiStatusExtractor implements ApiExtractor {

    private static final String STATUS_DEPRECATED = "deprecated";
    private static final String STATUS_NORMAL = "normal";

    private final AnnotationResolver annotationResolver;
    private final JavaDocResolver javaDocResolver;

    public ApiStatusExtractor(AnnotationResolver annotationResolver, JavaDocResolver javaDocResolver) {
        this.annotationResolver = annotationResolver;
        this.javaDocResolver = javaDocResolver;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        String status = extractStatus(method);
        if (status == null) {
            status = extractStatus(controllerClass);
        }
        api.setApiStatus(status != null ? status : STATUS_NORMAL);
    }

    /**
     * 从方法提取状态
     */
    private String extractStatus(PsiMethod method) {
        if (method == null) {
            return null;
        }

        if (annotationResolver.isDeprecated(method)) {
            return STATUS_DEPRECATED;
        }

        Boolean operationDeprecated = annotationResolver.getOperationDeprecated(method);
        if (operationDeprecated != null && operationDeprecated) {
            return STATUS_DEPRECATED;
        }

        if (javaDocResolver.hasDeprecatedTag(method)) {
            return STATUS_DEPRECATED;
        }

        return null;
    }

    /**
     * 从类提取状态
     */
    private String extractStatus(PsiClass clazz) {
        if (clazz == null) {
            return null;
        }

        if (annotationResolver.isDeprecated(clazz)) {
            return STATUS_DEPRECATED;
        }

        if (javaDocResolver.hasDeprecatedTag(clazz)) {
            return STATUS_DEPRECATED;
        }

        return null;
    }
}
