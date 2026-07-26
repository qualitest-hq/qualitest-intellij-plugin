package com.qualitest.scan.visitor;

import com.intellij.psi.*;
import com.qualitest.scan.resolver.AnnotationResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller 访问器。
 * 遍历 PsiClass 查找 Controller 类及其 API 方法（带有 HTTP 映射注解的方法）。
 * 注解识别统一委托 {@link AnnotationResolver}。
 */
public class ControllerVisitor {

    /** 注解解析器，所有注解检测均通过此实例完成 */
    private final AnnotationResolver annotationResolver = new AnnotationResolver();

    /**
     * 判断给定类是否为 Controller。
     * 判定依据：类上有 RestController、Controller、ControllerAdvice 或 RequestMapping 注解。
     *
     * @param clazz 待检测的 PSI 类
     * @return true 表示为 Controller
     */
    public boolean isController(PsiClass clazz) {
        if (clazz == null) {
            return false;
        }
        return annotationResolver.hasAnnotation(clazz, "RestController") ||
               annotationResolver.hasAnnotation(clazz, "Controller") ||
               annotationResolver.hasAnnotation(clazz, "ControllerAdvice") ||
               annotationResolver.hasAnnotation(clazz, "RequestMapping");
    }

    /**
     * 获取类中所有带 HTTP 映射注解的方法（API 方法）。
     *
     * @param clazz Controller 类
     * @return API 方法列表
     */
    public List<PsiMethod> getApiMethods(PsiClass clazz) {
        List<PsiMethod> apiMethods = new ArrayList<>();
        if (clazz == null) {
            return apiMethods;
        }

        for (PsiMethod method : clazz.getMethods()) {
            if (isApiMethod(method)) {
                apiMethods.add(method);
            }
        }
        return apiMethods;
    }

    /**
     * 判断给定方法是否为 API 方法。
     * 判定依据：方法上有 GetMapping、PostMapping、PutMapping、DeleteMapping、PatchMapping
     * 或 RequestMapping 注解，且不为构造函数。
     *
     * @param method 待检测的 PSI 方法
     * @return true 表示为 API 方法
     */
    public boolean isApiMethod(PsiMethod method) {
        if (method == null || method.isConstructor()) {
            return false;
        }
        String[] httpAnnotations = {
                "GetMapping", "PostMapping", "PutMapping",
                "DeleteMapping", "PatchMapping", "RequestMapping"
        };
        for (String annotationName : httpAnnotations) {
            if (annotationResolver.hasAnnotation(method, annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找给定 Java 文件中的所有 Controller 类。
     *
     * @param javaFile Java 源文件
     * @return Controller 类列表（不含则为空列表）
     */
    public List<PsiClass> findControllers(PsiJavaFile javaFile) {
        List<PsiClass> controllers = new ArrayList<>();
        if (javaFile == null) {
            return controllers;
        }
        for (PsiClass clazz : javaFile.getClasses()) {
            if (isController(clazz)) {
                controllers.add(clazz);
            }
        }
        return controllers;
    }
}
