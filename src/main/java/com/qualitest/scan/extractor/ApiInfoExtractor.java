package com.qualitest.scan.extractor;

import com.qualitest.QualiTestConstants;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.FieldExportFilter;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.resolver.TypeResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

/**
 * API信息提取器门面
 * 协调所有提取器完成API信息提取
 *
 * @author qualitest
 */
public class ApiInfoExtractor {

    private final AnnotationResolver annotationResolver;
    private final JavaDocResolver javaDocResolver;
    private final TypeResolver typeResolver;

    private final ApiStatusExtractor statusExtractor;
    private final ApiGroupExtractor groupExtractor;
    private final ApiNameExtractor nameExtractor;
    private final ApiDescriptionExtractor descriptionExtractor;
    private final ApiPathExtractor pathExtractor;
    private final RequestConfigExtractor requestExtractor;
    private final ResponseConfigExtractor responseExtractor;

    public ApiInfoExtractor() {
        this(QualiTestConstants.DEFAULT_GROUP_TAG, false, true);
    }

    public ApiInfoExtractor(String groupTag) {
        this(groupTag, false, true);
    }

    public ApiInfoExtractor(String groupTag, boolean ignoreFirstGroupLevel) {
        this(groupTag, ignoreFirstGroupLevel, true);
    }

    /**
     * @param excludeJsonIgnoreFields 是否在上传时排除 {@code @JsonIgnore} 字段，下发给请求/响应提取器
     */
    public ApiInfoExtractor(String groupTag, boolean ignoreFirstGroupLevel, boolean excludeJsonIgnoreFields) {
        this.annotationResolver = new AnnotationResolver();
        this.javaDocResolver = new JavaDocResolver(groupTag);
        this.typeResolver = new TypeResolver();
        // 请求 query、body 与响应 schema 共用同一套字段导出规则
        FieldExportFilter fieldExportFilter = new FieldExportFilter(annotationResolver, excludeJsonIgnoreFields);

        this.statusExtractor = new ApiStatusExtractor(annotationResolver, javaDocResolver);
        this.groupExtractor = new ApiGroupExtractor(annotationResolver, javaDocResolver, ignoreFirstGroupLevel);
        this.nameExtractor = new ApiNameExtractor(annotationResolver, javaDocResolver);
        this.descriptionExtractor = new ApiDescriptionExtractor(javaDocResolver);
        this.pathExtractor = new ApiPathExtractor(annotationResolver);
        this.requestExtractor = new RequestConfigExtractor(annotationResolver, typeResolver, javaDocResolver, fieldExportFilter);
        this.responseExtractor = new ResponseConfigExtractor(annotationResolver, typeResolver, javaDocResolver, fieldExportFilter);
    }

    /**
     * 从Controller类和方法中提取完整的API信息
     *
     * @param controllerClass Controller类
     * @param method API方法
     * @return 扫描到的API信息
     */
    @NotNull
    public ScannedApi extract(PsiClass controllerClass, PsiMethod method) {
        ScannedApi api = ScannedApi.builder().build();

        statusExtractor.extract(api, controllerClass, method);
        groupExtractor.extract(api, controllerClass, method);
        nameExtractor.extract(api, controllerClass, method);
        descriptionExtractor.extract(api, controllerClass, method);
        pathExtractor.extract(api, controllerClass, method);
        requestExtractor.extract(api, controllerClass, method);
        responseExtractor.extract(api, controllerClass, method);

        return api;
    }

    public AnnotationResolver getAnnotationResolver() {
        return annotationResolver;
    }

    public JavaDocResolver getJavaDocResolver() {
        return javaDocResolver;
    }

    public TypeResolver getTypeResolver() {
        return typeResolver;
    }
}
