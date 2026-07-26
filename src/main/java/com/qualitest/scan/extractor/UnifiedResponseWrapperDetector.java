package com.qualitest.scan.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;

import java.util.Set;

/**
 * 识别常见统一响应包装类型（R、ApiResult 等），并从泛型链中取「业务数据」类型。
 */
final class UnifiedResponseWrapperDetector {

    static final int MAX_RESPONSE_WRAPPER_UNWRAP_DEPTH = 5;

    private static final Set<String> UNIFIED_RESPONSE_WRAPPER_SIMPLE_NAMES = Set.of(
            "R",
            "AjaxResult",
            "ApiResult",
            "CommonResult",
            "BaseResult"
    );

    boolean isUnifiedResponseWrapper(PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }

        PsiType[] parameters = classType.getParameters();
        String canonicalText = type.getCanonicalText();
        PsiClass resolved = classType.resolve();
        String simpleName = resolved != null ? resolved.getName() : null;

        // com.qualitest 包下泛型 R&lt;T&gt;：依赖 canonical 文本匹配
        if (canonicalText.startsWith("com.qualitest.") && canonicalText.contains("R<")) {
            return parameters.length > 0;
        }

        // R、AjaxResult：多泛型参数时仅使用第一个实参作为解包目标
        if ("R".equals(canonicalText) || canonicalText.endsWith(".R") ||
                "AjaxResult".equals(canonicalText) || canonicalText.endsWith(".AjaxResult")) {
            return parameters.length > 0;
        }

        if (simpleName != null && UNIFIED_RESPONSE_WRAPPER_SIMPLE_NAMES.contains(simpleName)) {
            return parameters.length == 1;
        }

        return false;
    }

    PsiType extractDataType(PsiType returnType) {
        PsiType current = returnType;
        for (int depth = 0; depth < MAX_RESPONSE_WRAPPER_UNWRAP_DEPTH && current != null; depth++) {
            if (!isUnifiedResponseWrapper(current)) {
                break;
            }
            PsiType[] parameters = getGenericParameters(current);
            if (parameters.length == 0) {
                break;
            }
            current = parameters[0];
        }
        return current != null ? current : returnType;
    }

    private static PsiType[] getGenericParameters(PsiType type) {
        if (type instanceof PsiClassType classType) {
            return classType.getParameters();
        }
        return new PsiType[0];
    }
}
