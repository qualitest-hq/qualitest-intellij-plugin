package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * API分组提取器
 * 按优先级提取API分组：
 * 1. @api.group (JavaDoc自定义标签)
 * 2. @Tag 注解
 * 3. @RequestMapping 路径第一级
 *
 * @author qualitest
 */
public class ApiGroupExtractor implements ApiExtractor {

    private final AnnotationResolver annotationResolver;
    private final JavaDocResolver javaDocResolver;
    /** 为 true 时，最终分组去掉首个英文句点及其左侧（适用于其它插件写的模块前缀） */
    private final boolean ignoreFirstGroupLevel;

    public ApiGroupExtractor(AnnotationResolver annotationResolver, JavaDocResolver javaDocResolver) {
        this(annotationResolver, javaDocResolver, false);
    }

    public ApiGroupExtractor(
            AnnotationResolver annotationResolver,
            JavaDocResolver javaDocResolver,
            boolean ignoreFirstGroupLevel
    ) {
        this.annotationResolver = annotationResolver;
        this.javaDocResolver = javaDocResolver;
        this.ignoreFirstGroupLevel = ignoreFirstGroupLevel;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        String group = extractGroup(method);
        if (group == null) {
            group = extractGroup(controllerClass);
        }
        api.setApiGroup(applyIgnoreFirstLevel(group));
    }

    /**
     * 按设置去掉分组的第一级（第一个 {@code .} 之前的片段）。
     */
    private String applyIgnoreFirstLevel(String group) {
        if (group == null || group.isEmpty()) {
            return group;
        }
        String g = group.trim();
        if (!ignoreFirstGroupLevel) {
            return g;
        }
        int dot = g.indexOf('.');
        if (dot <= 0 || dot >= g.length() - 1) {
            return g;
        }
        return g.substring(dot + 1).trim();
    }

    /**
     * 从方法提取分组
     */
    private String extractGroup(PsiMethod method) {
        if (method == null) {
            return null;
        }

        String apiGroup = javaDocResolver.getApiGroup(method);
        if (apiGroup != null && !apiGroup.isEmpty()) {
            return normalizeGroup(apiGroup);
        }

        String tagName = annotationResolver.getTagName(method);
        if (tagName != null && !tagName.isEmpty()) {
            return tagName;
        }

        return null;
    }

    /**
     * 从类提取分组
     */
    private String extractGroup(PsiClass controllerClass) {
        if (controllerClass == null) {
            return null;
        }

        String apiGroup = javaDocResolver.getApiGroup(controllerClass);
        if (apiGroup != null && !apiGroup.isEmpty()) {
            return normalizeGroup(apiGroup);
        }

        String tagName = annotationResolver.getTagName(controllerClass);
        if (tagName != null && !tagName.isEmpty()) {
            return tagName;
        }

        String classPath = extractClassPath(controllerClass);
        if (classPath != null && !classPath.isEmpty()) {
            return classPath;
        }

        return controllerClass.getName();
    }

    /**
     * 从@RequestMapping提取路径第一级作为分组
     */
    private String extractClassPath(PsiClass controllerClass) {
        String path = annotationResolver.getAnnotationAttributeValue(
                annotationResolver.getAnnotation(controllerClass, "RequestMapping"),
                "value"
        );

        if (path == null || path.isEmpty()) {
            path = annotationResolver.getAnnotationAttributeValue(
                    annotationResolver.getAnnotation(controllerClass, "RequestMapping"),
                    "path"
            );
        }

        if (path != null && !path.isEmpty()) {
            return extractFirstPathSegment(path);
        }

        return null;
    }

    /**
     * 提取路径的第一段
     */
    private String extractFirstPathSegment(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        path = path.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }

        return path.isEmpty() ? null : path;
    }

    /**
     * 标准化分组名称
     */
    private String normalizeGroup(String group) {
        if (group == null) {
            return null;
        }
        return group.trim();
    }
}
