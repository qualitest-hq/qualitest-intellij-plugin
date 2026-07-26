package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.List;

/**
 * API路径提取器
 * 组合Controller类的@RequestMapping和方法上的@GetMapping等注解
 *
 * @author qualitest
 */
public class ApiPathExtractor implements ApiExtractor {

    private final AnnotationResolver annotationResolver;

    public ApiPathExtractor(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        String path = extractPath(controllerClass, method);
        api.setApiPath(path);
    }

    /**
     * 提取完整路径
     */
    private String extractPath(PsiClass controllerClass, PsiMethod method) {
        String classPath = getClassPath(controllerClass);
        String methodPath = getMethodPath(method);

        StringBuilder fullPath = new StringBuilder();

        if (classPath != null && !classPath.isEmpty()) {
            fullPath.append(classPath);
        }

        if (methodPath != null && !methodPath.isEmpty()) {
            if (fullPath.length() > 0 && !methodPath.startsWith("/")) {
                if (!fullPath.toString().endsWith("/")) {
                    fullPath.append("/");
                }
            }
            fullPath.append(methodPath);
        }

        String result = fullPath.toString();
        return result.isEmpty() ? "/" : normalizePath(result);
    }

    /**
     * 获取类级别路径
     */
    private String getClassPath(PsiClass controllerClass) {
        if (controllerClass == null) {
            return null;
        }

        PsiAnnotation requestMapping = annotationResolver.getAnnotation(controllerClass, "RequestMapping");
        if (requestMapping == null) {
            return null;
        }

        List<String> paths = annotationResolver.getAnnotationPathValues(requestMapping);
        if (!paths.isEmpty()) {
            return paths.get(0);
        }

        String value = annotationResolver.getAnnotationAttributeValue(requestMapping, "value");
        if (value != null && !value.isEmpty()) {
            return value;
        }

        String path = annotationResolver.getAnnotationAttributeValue(requestMapping, "path");
        return path;
    }

    /**
     * 获取方法级别路径
     */
    private String getMethodPath(PsiMethod method) {
        if (method == null) {
            return null;
        }

        String httpMappingAnnotation = annotationResolver.getHttpMappingAnnotation(method);
        if (httpMappingAnnotation == null) {
            return null;
        }

        PsiAnnotation annotation = annotationResolver.getAnnotation(method, httpMappingAnnotation);
        if (annotation == null) {
            return null;
        }

        List<String> paths = annotationResolver.getAnnotationPathValues(annotation);
        if (!paths.isEmpty()) {
            return paths.get(0);
        }

        String value = annotationResolver.getAnnotationAttributeValue(annotation, "value");
        if (value != null && !value.isEmpty()) {
            return value;
        }

        String path = annotationResolver.getAnnotationAttributeValue(annotation, "path");
        return path;
    }

    /**
     * 标准化路径
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        path = path.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
