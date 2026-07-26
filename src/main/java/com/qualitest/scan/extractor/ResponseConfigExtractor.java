package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ResponseConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.FieldExportFilter;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.resolver.TypeResolver;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;

import java.util.*;

/**
 * 响应配置提取器：从控制器方法返回类型生成 {@link ResponseConfig}。
 * <p>
 * 产出 {@code configVersion:1} 与 {@code responses[]} 结构；每条含稳定 id、
 * httpStatus=200、contentType=json 及由 PSI 推导的 JSON Schema。
 */
public class ResponseConfigExtractor implements ApiExtractor {

    private static final int DEFAULT_HTTP_STATUS = 200;
    private static final String DEFAULT_CONTENT_TYPE = "json";

    private final AnnotationResolver annotationResolver;
    private final TypeResolver typeResolver;
    private final JavaDocResolver javaDocResolver;
    /** 构建响应 JSON schema 时，按插件设置跳过 @JsonIgnore 字段 */
    private final FieldExportFilter fieldExportFilter;
    private final UnifiedResponseWrapperDetector unifiedWrapper = new UnifiedResponseWrapperDetector();

    public ResponseConfigExtractor(AnnotationResolver annotationResolver, TypeResolver typeResolver,
                                   JavaDocResolver javaDocResolver, FieldExportFilter fieldExportFilter) {
        this.annotationResolver = annotationResolver;
        this.typeResolver = typeResolver;
        this.javaDocResolver = javaDocResolver;
        // 控制响应 JSON schema 是否包含 @JsonIgnore 标注的字段
        this.fieldExportFilter = fieldExportFilter;
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        api.setResponseConfig(extractResponseConfig(api, method));
    }

    private ResponseConfig extractResponseConfig(ScannedApi api, PsiMethod method) {
        if (method == null) {
            return buildResponseConfig(api, "成功", null);
        }

        PsiType returnType = method.getReturnType();
        String name = resolveResponseName(method);
        Map<String, Object> schema = returnType != null ? extractResponseSchema(returnType) : null;
        return buildResponseConfig(api, name, schema);
    }

    private String resolveResponseName(PsiMethod method) {
        String returnDesc = javaDocResolver.getReturnDescription(method);
        if (returnDesc != null && !returnDesc.isEmpty()) {
            return returnDesc;
        }
        String summary = annotationResolver.getOperationSummary(method);
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }
        String javaDoc = javaDocResolver.getDescription(method);
        if (javaDoc != null && !javaDoc.isEmpty()) {
            return javaDoc;
        }
        return "成功";
    }

    /** 组装单条 responses 项；id 由方法、路径、状态码、内容类型哈希生成 */
    private ResponseConfig buildResponseConfig(ScannedApi api, String name, Map<String, Object> schema) {
        String httpMethod = api != null && api.getRequestConfig() != null
                ? api.getRequestConfig().getMethod() : "GET";
        String apiPath = api != null ? api.getApiPath() : null;
        String id = ResponseConfig.buildStableResponseId(
                httpMethod, apiPath, DEFAULT_HTTP_STATUS, DEFAULT_CONTENT_TYPE);

        ResponseConfig.ResponseItem item = ResponseConfig.ResponseItem.builder()
                .id(id)
                .name(name)
                .httpStatus(DEFAULT_HTTP_STATUS)
                .contentType(DEFAULT_CONTENT_TYPE)
                .schema(schema)
                .example(null)
                .build();

        return ResponseConfig.builder()
                .responses(List.of(item))
                .build();
    }

    private Map<String, Object> extractResponseSchema(PsiType returnType) {
        Set<String> visiting = new HashSet<>();
        if (returnType instanceof PsiClassType classType && unifiedWrapper.isUnifiedResponseWrapper(classType)) {
            return extractObjectSchema(returnType, visiting);
        }
        return extractDataSchema(returnType, visiting);
    }

    private Map<String, Object> extractDataSchema(PsiType returnType, Set<String> visiting) {
        PsiType dataType = unifiedWrapper.extractDataType(returnType);
        if (dataType == null) {
            return new HashMap<>();
        }
        if (isJavaLangVoid(dataType)) {
            return null;
        }

        TypeResolver.TypeMapping typeMapping = typeResolver.resolveType(dataType);
        return extractSchemaByType(dataType, typeMapping, visiting);
    }

    private Map<String, Object> extractSchemaByType(
            PsiType dataType,
            TypeResolver.TypeMapping typeMapping,
            Set<String> visiting) {
        String type = typeMapping.getType();
        if ("object".equals(type)) {
            return extractObjectSchema(dataType, visiting);
        }
        if ("array".equals(type)) {
            return extractArraySchema(dataType, visiting);
        }
        return buildPrimitiveSchema(typeMapping);
    }

    private static Map<String, Object> cyclicObjectSchema() {
        return Map.of("type", "object");
    }

    private Map<String, Object> extractObjectSchema(PsiType type, Set<String> visiting) {
        String typeKey = resolveSchemaTypeKey(type);
        if (typeKey != null && visiting.contains(typeKey)) {
            return cyclicObjectSchema();
        }
        if (typeKey != null) {
            visiting.add(typeKey);
        }
        try {
            return buildObjectSchema(type, visiting);
        } finally {
            if (typeKey != null) {
                visiting.remove(typeKey);
            }
        }
    }

    /**
     * 根据返回类型递归生成 object 类型的 JSON Schema。
     * 遍历类层次中的实例字段时，跳过 static、transient 以及带 @JsonIgnore 的字段。
     */
    private Map<String, Object> buildObjectSchema(PsiType type, Set<String> visiting) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        if (type instanceof PsiClassType classType) {
            PsiClass clazz = classType.resolve();
            if (clazz != null) {
                PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
                PsiSubstitutor substitutor =
                        resolveResult != null ? resolveResult.getSubstitutor() : PsiSubstitutor.EMPTY;
                for (PsiClass current = clazz; current != null && !isJavaLangObject(current);
                     current = current.getSuperClass()) {
                    for (PsiField field : current.getFields()) {
                        if (field.hasModifierProperty(PsiModifier.STATIC) ||
                            field.hasModifierProperty(PsiModifier.TRANSIENT) ||
                            !fieldExportFilter.shouldExportField(field)) {
                            continue;
                        }
                        String fieldName = field.getName();
                        if (properties.containsKey(fieldName)) {
                            continue;
                        }

                        PsiType fieldType = resolveFieldTypeInSite(field, clazz, substitutor);
                        if (isJavaLangVoid(fieldType)) {
                            continue;
                        }

                        TypeResolver.TypeMapping fieldMapping = typeResolver.resolveType(fieldType);

                        Map<String, Object> fieldSchema = new HashMap<>();
                        fieldSchema.putAll(extractSchemaByType(fieldType, fieldMapping, visiting));

                        String description = annotationResolver.getSchemaAttribute(field, "description");
                        if (description == null || description.isEmpty()) {
                            description = javaDocResolver.getFieldDescription(field);
                        }
                        if (description != null && !description.isEmpty()) {
                            fieldSchema.put("description", description);
                        }

                        properties.put(fieldName, fieldSchema);
                    }
                }
            }
        }

        schema.put("properties", properties);
        return schema;
    }

    private static boolean isJavaLangObject(PsiClass c) {
        return c != null && "java.lang.Object".equals(c.getQualifiedName());
    }

    private static boolean isJavaLangVoid(PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }
        PsiClass resolved = classType.resolve();
        return resolved != null && "java.lang.Void".equals(resolved.getQualifiedName());
    }

    private static PsiType resolveFieldTypeInSite(
            PsiField field,
            PsiClass siteClass,
            PsiSubstitutor siteSubstitutor) {
        PsiClass owner = field.getContainingClass();
        if (owner == null) {
            return siteSubstitutor.substitute(field.getType());
        }
        if (owner.equals(siteClass)) {
            return siteSubstitutor.substitute(field.getType());
        }
        PsiSubstitutor forOwner = TypeConversionUtil.getSuperClassSubstitutor(owner, siteClass, siteSubstitutor);
        return forOwner.substitute(field.getType());
    }

    private static String resolveSchemaTypeKey(PsiType type) {
        if (type instanceof PsiClassType classType) {
            PsiClass resolved = classType.resolve();
            if (resolved != null) {
                String qualifiedName = resolved.getQualifiedName();
                if (qualifiedName != null && !qualifiedName.isEmpty()) {
                    return qualifiedName;
                }
            }
        }
        String canonical = type.getCanonicalText();
        return canonical == null || canonical.isEmpty() ? null : canonical;
    }

    private Map<String, Object> extractArraySchema(PsiType type, Set<String> visiting) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "array");

        PsiType elementType = typeResolver.getCollectionElementType(type);
        if (elementType != null) {
            TypeResolver.TypeMapping elementMapping = typeResolver.resolveType(elementType);
            schema.put("items", extractSchemaByType(elementType, elementMapping, visiting));
        } else {
            schema.put("items", Map.of("type", "object"));
        }
        return schema;
    }

    private Map<String, Object> buildPrimitiveSchema(TypeResolver.TypeMapping typeMapping) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", typeMapping.getType());
        if (typeMapping.getFormat() != null) {
            schema.put("format", typeMapping.getFormat());
        }
        return schema;
    }
}
