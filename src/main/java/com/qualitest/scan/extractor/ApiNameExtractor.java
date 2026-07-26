package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * API名称提取器
 * 按优先级提取API名称：
 * 1. @Operation.summary
 * 2. JavaDoc第一行
 * 3. 方法名
 *
 * @author qualitest
 */
public class ApiNameExtractor implements ApiExtractor {

    private final AnnotationResolver annotationResolver;
    private final JavaDocResolver javaDocResolver;

    public ApiNameExtractor(AnnotationResolver annotationResolver, JavaDocResolver javaDocResolver) {
        this.annotationResolver = annotationResolver;
        this.javaDocResolver = javaDocResolver;
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        String name = extractName(method);
        api.setApiName(name);
    }

    /**
     * 从方法提取名称
     */
    private String extractName(PsiMethod method) {
        if (method == null) {
            return null;
        }

        String summary = annotationResolver.getOperationSummary(method);
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }

        String javaDoc = javaDocResolver.getDescription(method);
        if (javaDoc != null && !javaDoc.isEmpty()) {
            return javaDoc;
        }

        return method.getName();
    }
}
