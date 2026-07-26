package com.qualitest.scan.resolver;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.*;

/**
 * 注解解析器
 * 用于从PSI元素中提取注解信息
 *
 * @author qualitest
 */
public class AnnotationResolver {

    private static final Map<String, String> HTTP_METHOD_ANNOTATIONS = Map.of(
            "GetMapping", "GET",
            "PostMapping", "POST",
            "PutMapping", "PUT",
            "DeleteMapping", "DELETE",
            "PatchMapping", "PATCH",
            "RequestMapping", "GET"
    );

    /**
     * 获取方法上的HTTP映射注解
     *
     * @param method PSI方法
     * @return HTTP方法注解名称 (如 "GetMapping")
     */
    public String getHttpMappingAnnotation(PsiMethod method) {
        for (String annotationName : HTTP_METHOD_ANNOTATIONS.keySet()) {
            if (hasAnnotation(method, annotationName)) {
                return annotationName;
            }
        }
        return null;
    }

    /**
     * 获取HTTP方法类型
     *
     * @param method PSI方法
     * @return HTTP方法 (GET, POST, PUT, DELETE, PATCH)
     */
    public String getHttpMethod(PsiMethod method) {
        String annotationName = getHttpMappingAnnotation(method);
        if (annotationName != null) {
            String defaultMethod = HTTP_METHOD_ANNOTATIONS.get(annotationName);

            PsiAnnotation annotation = getAnnotation(method, annotationName);
            if (annotation != null) {
                String methodAttr = getAnnotationAttributeValue(annotation, "method");
                if (methodAttr != null) {
                    String normalized = normalizeSpringRequestMethodExpression(methodAttr);
                    if (normalized != null) {
                        return normalized;
                    }
                }
            }
            return defaultMethod;
        }
        return null;
    }

    /**
     * 读取注解上指定属性的源码文本值（已去掉引号）。
     * annotation 为 null 时直接返回 null，避免上层在注解缺失时 NPE。
     *
     * @param annotation    注解，可为 null
     * @param attributeName 属性名（如 value、path、consumes、required）
     * @return 属性值；无该属性或不存在注解时返回 null
     */
    public String getAnnotationAttributeValue(PsiAnnotation annotation, String attributeName) {
        if (annotation == null) {
            return null;
        }
        PsiNameValuePair[] pairs = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair pair : pairs) {
            String name = pair.getName();
            // 仅当请求 value 属性时，才接受无名参数（如 @GetMapping("/list")）。
            // 否则会把路径误读成 method/consumes 等属性，导致方法显示为 /LIST 这类错误值。
            boolean unnamedValue = name == null && "value".equals(attributeName);
            if (unnamedValue || attributeName.equals(name)) {
                PsiAnnotationMemberValue value = pair.getValue();
                if (value != null) {
                    String text = value.getText();
                    return stripQuotes(text);
                }
            }
        }
        return null;
    }

    /**
     * 获取注解的所有路径值
     *
     * @param annotation 注解
     * @return 路径值列表
     */
    public List<String> getAnnotationPathValues(PsiAnnotation annotation) {
        List<String> paths = new ArrayList<>();

        PsiNameValuePair[] pairs = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair pair : pairs) {
            String name = pair.getName();
            if (name == null || "value".equals(name) || "path".equals(name)) {
                PsiAnnotationMemberValue value = pair.getValue();
                if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
                    for (PsiAnnotationMemberValue element : arrayValue.getInitializers()) {
                        String text = element.getText();
                        if (text != null) {
                            paths.add(stripQuotes(text));
                        }
                    }
                } else if (value != null) {
                    String text = value.getText();
                    if (text != null) {
                        paths.add(stripQuotes(text));
                    }
                }
            }
        }
        return paths;
    }

    /**
     * 检查元素是否有指定注解
     *
     * @param element PSI元素
     * @param annotationName 注解名称
     * @return 是否存在
     */
    public boolean hasAnnotation(PsiElement element, String annotationName) {
        return getAnnotation(element, annotationName) != null;
    }

    /**
     * 获取元素上的指定注解
     *
     * @param element PSI元素
     * @param annotationName 注解名称
     * @return 注解对象
     */
    public PsiAnnotation getAnnotation(PsiElement element, String annotationName) {
        PsiAnnotation[] annotations = PsiTreeUtil.getChildrenOfType(element, PsiAnnotation.class);
        PsiAnnotation annotation = findByName(annotations, annotationName);
        if (annotation != null) {
            return annotation;
        }

        if (element instanceof PsiModifierListOwner owner) {
            return findByName(owner.getAnnotations(), annotationName);
        }
        return null;
    }

    /**
     * 获取元素的直接注解
     */
    public PsiAnnotation getDirectAnnotation(PsiModifierListOwner element, String annotationName) {
        PsiModifierList modifierList = element.getModifierList();
        if (modifierList == null) {
            return null;
        }
        return findByName(modifierList.getAnnotations(), annotationName);
    }

    /**
     * 检查是否有@Deprecated注解
     */
    public boolean isDeprecated(PsiModifierListOwner element) {
        return hasAnnotation(element, "Deprecated");
    }

    /**
     * 判断字段是否标注 Jackson 的 {@code @JsonIgnore}。
     * <p>
     * 用于上传前过滤：该注解表示字段不参与 JSON 序列化，通常也不应作为客户端可传的
     * 查询参数或请求体字段出现在质衡文档中。支持全限定名与简写名两种写法。
     */
    public boolean hasJsonIgnore(PsiField field) {
        return field != null && hasAnnotation(field, "JsonIgnore");
    }

    /**
     * 获取@Operation.deprecated值
     */
    public Boolean getOperationDeprecated(PsiMethod method) {
        String value = getAnnotationAttributeValue(method, "Operation", "deprecated");
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    /**
     * 获取@Operation.summary值
     */
    public String getOperationSummary(PsiMethod method) {
        return getAnnotationAttributeValue(method, "Operation", "summary");
    }

    /**
     * 获取@Tag.name值
     */
    public String getTagName(PsiModifierListOwner element) {
        return getAnnotationAttributeValue(element, "Tag", "name");
    }

    /**
     * 获取@Schema注解的属性
     */
    public Map<String, String> getSchemaProperties(PsiAnnotation annotation) {
        Map<String, String> properties = new HashMap<>();
        if (annotation == null) {
            return properties;
        }

        for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
            String name = pair.getName();
            PsiAnnotationMemberValue value = pair.getValue();
            if (name != null && value != null) {
                properties.put(name, stripQuotes(value.getText()));
            }
        }
        return properties;
    }

    /**
     * 获取@Schema注解属性
     */
    public String getSchemaAttribute(PsiModifierListOwner element, String attribute) {
        return getAnnotationAttributeValue(element, "Schema", attribute);
    }

    /**
     * 获取@Parameter注解属性
     */
    public String getParameterAttribute(PsiModifierListOwner element, String attribute) {
        return getAnnotationAttributeValue(element, "Parameter", attribute);
    }

    /**
     * OpenAPI / Swagger 参数描述：{@code io.swagger.v3.oas.annotations.Parameter#description}，
     * 或 Swagger2 {@code io.swagger.annotations.ApiParam} 的 {@code value}/{@code notes}。
     */
    public String getSwaggerParameterDescription(PsiParameter parameter) {
        if (parameter == null) {
            return null;
        }
        String d = getParameterAttribute(parameter, "description");
        if (isMeaningfulDescription(d)) {
            return d.trim();
        }
        d = getAnnotationAttributeValue(parameter, "ApiParam", "value");
        if (isMeaningfulDescription(d)) {
            return d.trim();
        }
        d = getAnnotationAttributeValue(parameter, "ApiParam", "notes");
        if (isMeaningfulDescription(d)) {
            return d.trim();
        }
        return null;
    }

    /**
     * 合并参数说明：注解优先（Swagger/OpenAPI），否则使用 JavaDoc。
     */
    public String mergeParameterDescription(PsiParameter parameter, String javaDocDescription) {
        String swagger = getSwaggerParameterDescription(parameter);
        if (swagger != null && !swagger.isBlank()) {
            return swagger;
        }
        return javaDocDescription != null ? javaDocDescription : "";
    }

    private static boolean isMeaningfulDescription(String d) {
        return d != null && !d.isBlank();
    }

    private String getAnnotationAttributeValue(PsiElement element, String annotationName, String attribute) {
        PsiAnnotation annotation = getAnnotation(element, annotationName);
        if (annotation == null) {
            return null;
        }
        return getAnnotationAttributeValue(annotation, attribute);
    }

    /**
     * 在注解数组中按后缀匹配名称。
     * 使用 endsWith 是为了兼容全限定名与简写并存的场景。
     */
    private PsiAnnotation findByName(PsiAnnotation[] annotations, String annotationName) {
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.endsWith(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 将 {@code RequestMethod.POST}、{@code {RequestMethod.GET}} 等Psi文本规范为 {@code GET}/{@code POST}/…；
     * 避免整段 {@code toUpperCase()} 变成 {@code REQUESTMETHOD.POST}。
     */
    static String normalizeSpringRequestMethodExpression(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1).trim();
            int comma = s.indexOf(',');
            if (comma > 0) {
                s = s.substring(0, comma).trim();
            }
        }
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < s.length() - 1) {
            s = s.substring(lastDot + 1).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        return s.toUpperCase();
    }

    /**
     * 去除字符串两端的引号
     */
    private String stripQuotes(String text) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        // 仅移除成对首尾引号，避免误改注解表达式中的中间字符。
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
