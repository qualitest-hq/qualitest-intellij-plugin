package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * 从方法 JavaDoc 中提取摘要行之后、块标签之前的详细描述。
 */
public class ApiDescriptionExtractor implements ApiExtractor {

    private final JavaDocResolver javaDocResolver;

    public ApiDescriptionExtractor(JavaDocResolver javaDocResolver) {
        this.javaDocResolver = javaDocResolver;
    }

    @Override
    public int getPriority() {
        return 31;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        if (method == null) {
            return;
        }
        api.setApiDescription(javaDocResolver.getApiDescriptionBody(method));
    }
}
