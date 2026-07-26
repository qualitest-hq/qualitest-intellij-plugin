package com.qualitest.scan.resolver;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;

/**
 * 扫描上传时，按插件设置决定是否把 Java 模型字段写入接口文档。
 * <p>
 * 典型场景：查询参数 Bean（如 ApiParams）里由服务端注入的 accountId 会标注
 * {@code @JsonIgnore}，开启排除后该字段不会出现在 queryParams、请求体 schema、
 * 表单字段或响应 schema 中。
 */
public final class FieldExportFilter {

    /** 识别字段上的 Jackson 注解 */
    private final AnnotationResolver annotationResolver;

    /**
     * 为 true 时跳过带 {@code @JsonIgnore} 的字段；
     * 为 false 时忽略该注解，所有业务字段照常导出。
     */
    private final boolean excludeJsonIgnoreFields;

    /**
     * @param annotationResolver       用于检测字段是否标注 {@code @JsonIgnore}
     * @param excludeJsonIgnoreFields  是否排除带该注解的字段，来自插件设置页
     */
    public FieldExportFilter(AnnotationResolver annotationResolver, boolean excludeJsonIgnoreFields) {
        this.annotationResolver = annotationResolver;
        this.excludeJsonIgnoreFields = excludeJsonIgnoreFields;
    }

    /**
     * 请求侧模型字段是否应参与上传（query、JSON body、form）。
     * 同时排除 static/transient/合成字段，以及开启排除时的 {@code @JsonIgnore} 字段。
     */
    public boolean shouldExportModelField(PsiField field) {
        return isApiModelField(field) && shouldExportField(field);
    }

    /**
     * 判断单个字段是否应参与上传。
     * <p>
     * 关闭排除开关时恒为 true（仅校验 field 非空）；
     * 开启排除开关时，带 {@code @JsonIgnore} 的字段返回 false。
     */
    public boolean shouldExportField(PsiField field) {
        if (field == null) {
            return false;
        }
        if (!excludeJsonIgnoreFields) {
            return true;
        }
        return !annotationResolver.hasJsonIgnore(field);
    }

    /** 参与 API Schema 的模型字段：排除 static/transient 与合成字段。 */
    private static boolean isApiModelField(PsiField field) {
        if (field == null) {
            return false;
        }
        if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
            return false;
        }
        String name = field.getName();
        return name != null && !name.startsWith("this$") && !name.contains("$");
    }
}
