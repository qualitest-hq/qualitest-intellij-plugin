package com.qualitest.scan.extractor;

import com.qualitest.scan.model.*;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.FieldExportFilter;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.resolver.TypeResolver;
import com.intellij.psi.*;

import java.util.*;

/**
 * 请求配置提取器：从 Controller 方法扫描 HTTP 方法、路径/查询/请求头参数与请求体，写入 {@link RequestConfig}。
 * <p>
 * 文件上传：MultipartFile、Servlet Part（及数组/List）→ form-data 的 {@code type=file}；
 * 普通表单文本字段 → {@code string}/{@code integer} 等类型名。
 */
public class RequestConfigExtractor implements ApiExtractor {

    private final AnnotationResolver annotationResolver;
    private final TypeResolver typeResolver;
    private final JavaDocResolver javaDocResolver;
    /** 按插件设置过滤 @JsonIgnore 字段，作用于 query、JSON body、form 三类请求描述 */
    private final FieldExportFilter fieldExportFilter;

    public RequestConfigExtractor(AnnotationResolver annotationResolver, TypeResolver typeResolver,
                              JavaDocResolver javaDocResolver, FieldExportFilter fieldExportFilter) {
        this.annotationResolver = annotationResolver;
        this.typeResolver = typeResolver;
        this.javaDocResolver = javaDocResolver;
        // 控制 query 参数、JSON body schema、form 字段是否导出 @JsonIgnore 标注的字段
        this.fieldExportFilter = fieldExportFilter;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method) {
        RequestConfig requestConfig = RequestConfig.builder()
                .method(extractMethod(method))
                .pathParams(extractPathParams(method))
                .queryParams(extractQueryParams(method))
                .declaredHeaders(extractDeclaredHeaders(method))
                .body(extractBody(method))
                .build();

        api.setRequestConfig(requestConfig);
    }

    /**
     * 从方法注解中提取 HTTP 方法类型（GET/POST/PUT/DELETE/PATCH）。
     * 若无注解默认返回 GET。
     *
     * @param method PSI 方法
     * @return HTTP 方法字符串
     */
    private String extractMethod(PsiMethod method) {
        if (method == null) {
            return "GET";
        }
        return annotationResolver.getHttpMethod(method);
    }

    /**
     * 提取 @PathVariable 标注的路径参数。
     *
     * @param method PSI 方法
     * @return 路径参数列表
     */
    private List<ApiParameter> extractPathParams(PsiMethod method) {
        List<ApiParameter> pathParams = new ArrayList<>();
        if (method == null) {
            return pathParams;
        }

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (annotationResolver.hasAnnotation(parameter, "PathVariable")) {
                PsiType type = parameter.getType();
                String jd = javaDocResolver.getParamDescription(method, parameter.getName());
                String description = annotationResolver.mergeParameterDescription(parameter, jd);

                ApiParameter param = ApiParameter.builder()
                        .name(getParameterName(parameter))
                        .in("path")
                        .type(typeResolver.getTypeName(type))
                        .description(description)
                        .required(isPathVariableRequired(parameter))
                        .schema(typeResolver.resolveType(type).toSchema())
                        .build();

                pathParams.add(param);
            }
        }
        return pathParams;
    }

    /**
     * 提取查询参数：普通 {@code @RequestParam}、无注解的查询 Bean、以及无注解的简单类型隐式参数。
     * <p>
     * 文件类型参数不会进入 query；若方法已被识别为 multipart 上传，则其 {@code @RequestParam}
     * 与隐式简单参数改由 form-data 承载，此处全部跳过。
     * 查询侧必填：优先读 RequestParam.required，未写时再看 NotNull、NotBlank、NotEmpty。
     *
     * @param method PSI 方法
     * @return 查询参数列表
     */
    private List<ApiParameter> extractQueryParams(PsiMethod method) {
        List<ApiParameter> params = new ArrayList<>();
        if (method == null) {
            return params;
        }

        boolean multipartForm = usesMultipartForm(method);

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (!annotationResolver.hasAnnotation(parameter, "RequestParam")) {
                continue;
            }
            // 文件或 multipart 上传方法上的 RequestParam 写入 form-data，不进 queryParams
            if (isFileUploadType(parameter.getType()) || multipartForm) {
                continue;
            }
            PsiType type = parameter.getType();
            String jd = javaDocResolver.getParamDescription(method, parameter.getName());
            String description = annotationResolver.mergeParameterDescription(parameter, jd);
            String defaultValue = getRequestParamDefault(parameter);
            boolean required = isParameterRequired(parameter);

            ApiParameter param = ApiParameter.builder()
                    .name(getParameterName(parameter))
                    .in("query")
                    .value(defaultValue)
                    .type(typeResolver.getTypeName(type))
                    .description(description)
                    .required(required)
                    .schema(typeResolver.resolveType(type).toSchema())
                    .build();

            params.add(param);
        }

        // 无 @RequestParam 的对象形参：展开其字段作为查询条件（如列表筛选 Bean）
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (isQueryBeanParameter(parameter)) {
                params.addAll(extractQueryParamsFromBean(method, parameter));
            }
        }

        // 无注解的简单类型形参按查询参数处理；multipart 上传方法跳过（改入 form-data）
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (isImplicitRequestParam(parameter) && !multipartForm) {
                params.add(buildImplicitRequestParam(method, parameter));
            }
        }
        return params;
    }

    /**
     * 判断是否为框架注入、不应出现在接口描述中的参数类型。
     * <p>
     * 会跳过 Servlet 请求/响应对、Spring MVC 上下文对象、MultipartHttpServletRequest，
     * 以及 Principal、流等类型。
     * 业务上传用的 MultipartFile、Servlet Part 不跳过，需导出为文件字段。
     */
    private boolean isSkippableFrameworkParameter(PsiParameter parameter) {
        if (parameter == null) {
            return false;
        }
        PsiType type = parameter.getType();
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }
        PsiClass clazz = classType.resolve();
        if (clazz == null) {
            return false;
        }
        String qName = clazz.getQualifiedName();
        if (qName == null) {
            return false;
        }
        // Part 虽属 servlet 包，但是上传部件，要导出
        if (isServletPartType(qName)) {
            return false;
        }
        if (qName.startsWith("javax.servlet.") || qName.startsWith("jakarta.servlet.")) {
            return true;
        }
        if (qName.startsWith("org.springframework.web.context.request.")
                || qName.startsWith("org.springframework.web.servlet.mvc.support.")
                || qName.startsWith("org.springframework.ui.")
                || qName.startsWith("org.springframework.validation.")
                || qName.startsWith("org.springframework.http.HttpEntity")
                || qName.startsWith("org.springframework.http.ResponseEntity")) {
            return true;
        }
        // 只跳过多部分请求对象本身，不跳过 MultipartFile
        if ("org.springframework.web.multipart.MultipartHttpServletRequest".equals(qName)) {
            return true;
        }
        return qName.equals("java.security.Principal")
                || qName.equals("java.io.InputStream")
                || qName.equals("java.io.OutputStream")
                || qName.equals("java.io.Reader")
                || qName.equals("java.io.Writer");
    }

    /** 是否为 Servlet 上传部件类型（javax / jakarta 的 Part）。 */
    private static boolean isServletPartType(String qName) {
        return "javax.servlet.http.Part".equals(qName)
                || "jakarta.servlet.http.Part".equals(qName);
    }

    /** 是否为 Spring MultipartFile 全限定名。 */
    private static boolean isMultipartFileType(String qName) {
        return "org.springframework.web.multipart.MultipartFile".equals(qName);
    }

    /**
     * 是否为文件上传类型：单个 MultipartFile/Part，或数组、集合元素为上述类型。
     * 用于决定写入 form-data 的 {@code type=file}，且不进入 queryParams。
     */
    private boolean isFileUploadType(PsiType type) {
        if (type == null) {
            return false;
        }
        if (isSingleFileUploadType(type)) {
            return true;
        }
        return isMultipleFileUploadType(type);
    }

    /**
     * 是否为单个文件类型（非集合、非数组包装）。
     * 优先用全限定名判断；解析失败时对非泛型回退到简单名/规范名后缀。
     */
    private boolean isSingleFileUploadType(PsiType type) {
        String qName = resolveQualifiedName(type);
        if (qName != null && (isMultipartFileType(qName) || isServletPartType(qName))) {
            return true;
        }
        if (type == null || hasTypeArguments(type)) {
            return false;
        }
        // 类解析失败时的兜底识别，避免漏扫文件参数
        String presentable = type.getPresentableText();
        String canonical = type.getCanonicalText();
        return "MultipartFile".equals(presentable)
                || "Part".equals(presentable)
                || (canonical != null && (canonical.endsWith(".MultipartFile") || canonical.endsWith(".Part")));
    }

    /**
     * 是否为多文件类型：数组或集合的元素为 MultipartFile/Part。
     * 扫成单个 form-data 项 {@code type=file}，并在描述中注明 multiple。
     */
    private boolean isMultipleFileUploadType(PsiType type) {
        if (type == null || isSingleFileUploadType(type)) {
            return false;
        }
        PsiType element = typeResolver.getCollectionElementType(type);
        return element != null && isSingleFileUploadType(element);
    }

    /** 类型是否带泛型参数（如 {@code List<T>}），用于避免把集合本体误判为单文件类型。 */
    private static boolean hasTypeArguments(PsiType type) {
        return type instanceof PsiClassType classType && classType.getParameters().length > 0;
    }

    /** 解析引用类型的全限定类名；非类类型或无法解析时返回 null。 */
    private static String resolveQualifiedName(PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }
        PsiClass clazz = classType.resolve();
        return clazz != null ? clazz.getQualifiedName() : null;
    }

    /**
     * 是否为 JDK / Servlet API / Spring 框架类型。
     * 用于排除不宜当作业务表单 Bean 展开的类型。
     */
    private static boolean isFrameworkOrJdkType(String qName) {
        return qName == null
                || qName.startsWith("java.")
                || qName.startsWith("javax.")
                || qName.startsWith("jakarta.")
                || qName.startsWith("org.springframework.");
    }

    /** 参数上是否带有给定注解名中的任意一个（按简写名匹配）。 */
    private boolean hasAnyAnnotation(PsiParameter parameter, String... annotationNames) {
        if (parameter == null || annotationNames == null) {
            return false;
        }
        for (String name : annotationNames) {
            if (annotationResolver.hasAnnotation(parameter, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法是否应按 multipart/form-data 建模。
     * 依据：存在文件类型参数，或存在带文件字段的表单 Bean（如 {@code @ModelAttribute}）。
     * 已标注 {@code @RequestBody} 的参数另行走 Content-Type 判断，此处跳过。
     */
    private boolean usesMultipartForm(PsiMethod method) {
        if (method == null) {
            return false;
        }
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (annotationResolver.hasAnnotation(parameter, "RequestBody")) {
                continue;
            }
            if (isFileUploadType(parameter.getType()) || isFormBeanWithFileFields(parameter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为「含文件字段、应展开为 form-data」的 Bean 形参。
     * 排除已有 RequestParam/PathVariable 等绑定注解的参数；通常对应 ModelAttribute 或无注解对象。
     */
    private boolean isFormBeanWithFileFields(PsiParameter parameter) {
        if (parameter == null || isSkippableFrameworkParameter(parameter)) {
            return false;
        }
        if (hasAnyAnnotation(parameter,
                "RequestParam", "PathVariable", "RequestHeader", "RequestBody", "RequestPart")) {
            return false;
        }
        return typeHasFileUploadField(parameter.getType());
    }

    /**
     * 业务 POJO 类型中是否存在可导出的文件上传字段。
     * JDK/框架类型直接返回 false，避免误判。
     */
    private boolean typeHasFileUploadField(PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }
        PsiClass clazz = classType.resolve();
        if (clazz == null || isFrameworkOrJdkType(clazz.getQualifiedName())) {
            return false;
        }
        for (PsiField field : clazz.getAllFields()) {
            if (!fieldExportFilter.shouldExportModelField(field)) {
                continue;
            }
            if (isFileUploadType(field.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为无注解、按请求参数绑定的简单类型形参。
     * 排除框架类型、文件类型、已有绑定注解的参数，以及查询 Bean / 含文件字段的表单 Bean。
     */
    private boolean isImplicitRequestParam(PsiParameter parameter) {
        if (parameter == null || isSkippableFrameworkParameter(parameter)) {
            return false;
        }
        if (isFileUploadType(parameter.getType())) {
            return false;
        }
        if (hasAnyAnnotation(parameter,
                "RequestParam", "PathVariable", "RequestHeader", "RequestBody",
                "RequestPart", "CookieValue", "MatrixVariable", "ModelAttribute")) {
            return false;
        }
        if (isQueryBeanParameter(parameter) || isFormBeanWithFileFields(parameter)) {
            return false;
        }
        TypeResolver.TypeMapping mapping = typeResolver.resolveType(parameter.getType());
        String jsonType = mapping.getType();
        return !"object".equals(jsonType) && !"array".equals(jsonType);
    }

    /** 将隐式简单类型形参组装为一条 query 参数。 */
    private ApiParameter buildImplicitRequestParam(PsiMethod method, PsiParameter parameter) {
        PsiType type = parameter.getType();
        String name = parameter.getName();
        String jd = javaDocResolver.getParamDescription(method, name);
        String description = annotationResolver.mergeParameterDescription(parameter, jd);
        return ApiParameter.builder()
                .name(name)
                .in("query")
                .type(typeResolver.getTypeName(type))
                .description(description)
                .required(isImplicitParamRequired(parameter))
                .schema(typeResolver.resolveType(type).toSchema())
                .build();
    }

    /** 隐式简单参数默认必填；带 {@code @Nullable} 时视为非必填。 */
    private boolean isImplicitParamRequired(PsiParameter parameter) {
        return !annotationResolver.hasAnnotation(parameter, "Nullable");
    }

    /**
     * 是否为应展开为 query 字段的对象形参（无绑定注解的业务 Bean）。
     * 含文件字段的表单 Bean 不走此路径，改为 form-data。
     */
    private boolean isQueryBeanParameter(PsiParameter parameter) {
        if (parameter == null || isSkippableFrameworkParameter(parameter)) {
            return false;
        }
        if (isFormBeanWithFileFields(parameter)) {
            return false;
        }
        if (hasAnyAnnotation(parameter,
                "RequestParam", "PathVariable", "RequestHeader", "RequestBody", "RequestPart")) {
            return false;
        }

        PsiType type = parameter.getType();
        if (!"object".equals(typeResolver.resolveType(type).getType())) {
            return false;
        }
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }
        PsiClass clazz = classType.resolve();
        return clazz != null && !isFrameworkOrJdkType(clazz.getQualifiedName());
    }

    /**
     * 将无注解或带 {@code @ModelAttribute} 的 Bean 形参展开为 query 参数列表。
     * 遍历 Bean 字段时按插件设置跳过 {@code @JsonIgnore} 字段（如服务端注入的 accountId），
     * 避免这些字段出现在客户端可见的查询参数中。
     */
    private List<ApiParameter> extractQueryParamsFromBean(PsiMethod method, PsiParameter beanParameter) {
        List<ApiParameter> result = new ArrayList<>();
        if (!(beanParameter.getType() instanceof PsiClassType classType)) {
            return result;
        }
        PsiClass beanClass = classType.resolve();
        if (beanClass == null) {
            return result;
        }

        for (PsiField field : beanClass.getAllFields()) {
            if (!fieldExportFilter.shouldExportModelField(field)) {
                continue;
            }
            String fieldName = field.getName();
            PsiType fieldType = field.getType();
            String description = annotationResolver.getSchemaAttribute(field, "description");
            if (description == null || description.isEmpty()) {
                description = javaDocResolver.getFieldDescription(field);
            }
            if (description == null || description.isEmpty()) {
                description = javaDocResolver.getParamDescription(method, fieldName);
            }

            ApiParameter param = ApiParameter.builder()
                    .name(fieldName)
                    .in("query")
                    .type(typeResolver.getTypeName(fieldType))
                    .description(description != null ? description : "")
                    .required(isRequiredField(field))
                    .schema(typeResolver.resolveType(fieldType).toSchema())
                    .build();
            result.add(param);
        }
        return result;
    }

    /**
     * 提取请求体配置。
     * <ol>
     *   <li>存在 {@code @RequestBody}：按 consumes / Content-Type 生成 json、urlencoded、form-data 等</li>
     *   <li>否则若判定为文件上传：按方法参数组装 form-data</li>
     *   <li>否则 body 为 none</li>
     * </ol>
     *
     * @param method PSI 方法
     * @return 请求体描述
     */
    private RequestBody extractBody(PsiMethod method) {
        if (method == null) {
            return RequestBody.none();
        }

        PsiParameter bodyParam = null;
        String contentType = null;

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (annotationResolver.hasAnnotation(parameter, "RequestBody")) {
                bodyParam = parameter;
                contentType = resolveContentType(method, parameter);
                break;
            }
        }

        if (bodyParam != null) {
            return buildBodyFromType(bodyParam.getType(), contentType);
        }
        if (usesMultipartForm(method)) {
            return RequestBody.formData(extractMultipartFormFromParams(method));
        }
        return RequestBody.none();
    }

    /**
     * 从方法参数组装 form-data 列表：
     * 含文件字段的 Bean 展开字段；文件类型参数输出 {@code type=file}；
     * 同方法上的文本 {@code @RequestParam} 一并写入 form-data。
     * 路径变量、请求头、Cookie 等非表单项跳过。
     */
    private List<RequestBody.FormDataItem> extractMultipartFormFromParams(PsiMethod method) {
        List<RequestBody.FormDataItem> items = new ArrayList<>();
        if (method == null) {
            return items;
        }

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (isSkippableFrameworkParameter(parameter)
                    || hasAnyAnnotation(parameter,
                    "PathVariable", "RequestHeader", "RequestBody", "CookieValue", "MatrixVariable")) {
                continue;
            }

            // ModelAttribute / 无注解、且含 MultipartFile 等字段的表单对象
            if (isFormBeanWithFileFields(parameter)) {
                items.addAll(extractFormDataItems(parameter.getType(), false));
                continue;
            }

            boolean requestPart = annotationResolver.hasAnnotation(parameter, "RequestPart");
            boolean requestParam = annotationResolver.hasAnnotation(parameter, "RequestParam");
            PsiType type = parameter.getType();

            // RequestPart / RequestParam 文件，或无绑定注解的裸文件参数
            if (isFileUploadType(type) && (requestPart || requestParam || !hasBindingAnnotation(parameter))) {
                items.add(buildFileFormDataItem(method, parameter));
                continue;
            }

            // multipart 方法上的文本 RequestParam（如业务分类）
            if (requestParam && !isFileUploadType(type)) {
                items.add(buildTextFormDataItem(method, parameter));
            }
        }
        return items;
    }

    /** 参数是否带有 Spring MVC 绑定注解（有则不再当作「裸文件参数」处理）。 */
    private boolean hasBindingAnnotation(PsiParameter parameter) {
        return hasAnyAnnotation(parameter,
                "RequestParam", "RequestPart", "PathVariable", "RequestHeader",
                "RequestBody", "ModelAttribute", "CookieValue", "MatrixVariable");
    }

    /** 组装一条文件型 form-data 项：{@code type=file}，多文件时补充 multiple 说明。 */
    private RequestBody.FormDataItem buildFileFormDataItem(PsiMethod method, PsiParameter parameter) {
        String name = getParameterName(parameter);
        String jd = javaDocResolver.getParamDescription(method, parameter.getName());
        String description = appendMultipleFileHint(
                annotationResolver.mergeParameterDescription(parameter, jd),
                parameter.getType());
        return new RequestBody.FormDataItem(
                name, "", "file", description != null ? description : "", isFileParameterRequired(parameter));
    }

    /** 组装一条文本/其它类型的 form-data 项（非文件）。 */
    private RequestBody.FormDataItem buildTextFormDataItem(PsiMethod method, PsiParameter parameter) {
        String name = getParameterName(parameter);
        String jd = javaDocResolver.getParamDescription(method, parameter.getName());
        String description = annotationResolver.mergeParameterDescription(parameter, jd);
        return new RequestBody.FormDataItem(
                name, "", resolveFormFieldType(parameter.getType(), false),
                description != null ? description : "", isParameterRequired(parameter));
    }

    /**
     * 判断文件参数是否必填。
     * RequestPart / RequestParam 未写 required 时默认必填；
     * 其它情况再看 NotNull、NotBlank、NotEmpty。
     */
    private boolean isFileParameterRequired(PsiParameter parameter) {
        if (annotationResolver.hasAnnotation(parameter, "RequestPart")) {
            return readRequiredAttribute(parameter, "RequestPart", true);
        }
        if (annotationResolver.hasAnnotation(parameter, "RequestParam")) {
            return readRequiredAttribute(parameter, "RequestParam", true);
        }
        return isRequiredField(parameter);
    }

    /**
     * 读取注解上的 required 属性。
     *
     * @param parameter          方法参数
     * @param annotationName     注解简写名
     * @param defaultWhenAbsent  注解上未写 required 时的默认值
     */
    private boolean readRequiredAttribute(PsiParameter parameter, String annotationName, boolean defaultWhenAbsent) {
        String required = annotationResolver.getAnnotationAttributeValue(
                annotationResolver.getAnnotation(parameter, annotationName),
                "required"
        );
        if (required != null) {
            return Boolean.parseBoolean(required);
        }
        return defaultWhenAbsent;
    }

    /**
     * 多文件类型时在描述中补充 multiple 提示；
     * 已有说明则追加 {@code (multiple)}，无说明则使用 {@code multiple files}。
     */
    private String appendMultipleFileHint(String description, PsiType type) {
        if (!isMultipleFileUploadType(type)) {
            return description;
        }
        if (description == null || description.isEmpty()) {
            return "multiple files";
        }
        if (!description.toLowerCase(Locale.ROOT).contains("multiple")) {
            return description + " (multiple)";
        }
        return description;
    }

    /**
     * 解析 form-data / urlencoded 字段的 type 字符串。
     * multipart 下文件类型为 {@code file}；其余走类型解析器（如 string、integer）。
     * urlencoded 模式不输出 file。
     */
    private String resolveFormFieldType(PsiType fieldType, boolean isUrlEncoded) {
        if (!isUrlEncoded && isFileUploadType(fieldType)) {
            return "file";
        }
        return typeResolver.getTypeName(fieldType);
    }

    /**
     * 根据 Content-Type 构建对应的 RequestBody。
     *
     * @param type         请求体参数类型
     * @param contentType  Content-Type 字符串
     * @return 对应的 RequestBody
     */
    private RequestBody buildBodyFromType(PsiType type, String contentType) {
        String normalized = contentType != null ? contentType : "application/json";
        // 统一做 contains 判断，兼容带 charset/boundary 的 Content-Type。
        if (normalized.contains("application/json")) {
            return buildJsonBody(type);
        }
        if (normalized.contains("application/x-www-form-urlencoded")) {
            return RequestBody.urlencoded(extractFormDataItems(type, true));
        }
        if (normalized.contains("multipart/form-data")) {
            return RequestBody.formData(extractFormDataItems(type, false));
        }
        if (normalized.contains("text/plain")) {
            return RequestBody.text("");
        }
        return RequestBody.none();
    }

    /**
     * JSON body：按参数类型生成根级 JSON Schema（含 List/数组元素类型）。
     */
    private RequestBody buildJsonBody(PsiType type) {
        TypeResolver.TypeMapping mapping = typeResolver.resolveType(type);
        Map<String, Object> schema = buildSchemaForType(type, mapping);
        return RequestBody.json(schema);
    }

    /**
     * 解析 Content-Type：优先从方法级 @RequestMapping/GET/POST... 获取，
     * 若无则从类级 @RequestMapping 获取，均无则返回 null。
     *
     * @param method    PSI 方法
     * @param parameter 请求体参数（仅用于类级注解回退查找）
     * @return Content-Type 字符串
     */
    private String resolveContentType(PsiMethod method, PsiParameter parameter) {
        // 1. 从方法级映射注解的 consumes 属性获取
        String httpMapping = annotationResolver.getHttpMappingAnnotation(method);
        if (httpMapping != null) {
            PsiAnnotation annotation = annotationResolver.getAnnotation(method, httpMapping);
            if (annotation != null) {
                String consumes = annotationResolver.getAnnotationAttributeValue(annotation, "consumes");
                if (consumes != null && !consumes.isEmpty()) {
                    return consumes;
                }
            }
        }

        // 2. 回退：类级 @RequestMapping
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            String classLevel = annotationResolver.getAnnotationAttributeValue(
                    annotationResolver.getAnnotation(containingClass, "RequestMapping"),
                    "consumes"
            );
            if (classLevel != null) {
                return classLevel;
            }
        }
        // 未声明 consumes 时返回 null，上层按 application/json 处理
        return null;
    }

    /**
     * 提取 @RequestHeader 标注的请求头参数。
     *
     * @param method PSI 方法
     * @return 请求头参数列表
     */
    private List<ApiParameter> extractDeclaredHeaders(PsiMethod method) {
        List<ApiParameter> headers = new ArrayList<>();
        if (method == null) {
            return headers;
        }

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (annotationResolver.hasAnnotation(parameter, "RequestHeader")) {
                PsiType type = parameter.getType();
                String name = getParameterName(parameter);
                String jd = javaDocResolver.getParamDescription(method, name);
                String description = annotationResolver.mergeParameterDescription(parameter, jd);

                ApiParameter header = ApiParameter.builder()
                        .name(name)
                        .in("header")
                        .type(typeResolver.getTypeName(type))
                        .description(description)
                        .required(isRequestHeaderRequired(parameter))
                        .schema(typeResolver.resolveType(type).toSchema())
                        .build();

                headers.add(header);
            }
        }
        return headers;
    }

    /**
     * 获取对外参数名：优先读 PathVariable / RequestParam / RequestPart / RequestHeader 的 value 或 name，
     * 都没有则用形参名。
     *
     * @param parameter PSI 参数
     * @return 参数名称
     */
    private String getParameterName(PsiParameter parameter) {
        String name = readParamAlias(annotationResolver.getAnnotation(parameter, "PathVariable"));
        if (name == null) {
            name = readParamAlias(annotationResolver.getAnnotation(parameter, "RequestParam"));
        }
        if (name == null) {
            name = readParamAlias(annotationResolver.getAnnotation(parameter, "RequestPart"));
        }
        if (name == null) {
            name = readParamAlias(annotationResolver.getAnnotation(parameter, "RequestHeader"));
        }
        return name != null ? name : parameter.getName();
    }

    /** 从注解的 value、name 属性读取参数别名；注解为 null 或属性为空时返回 null。 */
    private String readParamAlias(PsiAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        String alias = annotationResolver.getAnnotationAttributeValue(annotation, "value");
        if (alias == null || alias.isEmpty()) {
            alias = annotationResolver.getAnnotationAttributeValue(annotation, "name");
        }
        return (alias == null || alias.isEmpty()) ? null : alias;
    }

    /**
     * 获取 @RequestParam 的 defaultValue 属性。
     *
     * @param parameter PSI 参数
     * @return defaultValue 字符串，若无则返回 null
     */
    private String getRequestParamDefault(PsiParameter parameter) {
        return annotationResolver.getAnnotationAttributeValue(
                annotationResolver.getAnnotation(parameter, "RequestParam"),
                "defaultValue"
        );
    }

    /**
     * 判断 RequestParam 是否必填：有 required 属性用其值；
     * 未写 required 时根据 NotNull、NotBlank、NotEmpty 判断。
     *
     * @param parameter PSI 参数
     * @return true 表示必填
     */
    private boolean isParameterRequired(PsiParameter parameter) {
        String required = annotationResolver.getAnnotationAttributeValue(
                annotationResolver.getAnnotation(parameter, "RequestParam"),
                "required"
        );
        if (required != null) {
            return Boolean.parseBoolean(required);
        }
        return isRequiredField(parameter);
    }

    /** 请求头是否必填：读 RequestHeader.required，未写则看 NotNull、NotBlank、NotEmpty。 */
    private boolean isRequestHeaderRequired(PsiParameter parameter) {
        return readRequiredAttribute(parameter, "RequestHeader", isRequiredField(parameter));
    }

    /** 路径参数是否必填：读 PathVariable.required，未写则默认必填。 */
    private boolean isPathVariableRequired(PsiParameter parameter) {
        return readRequiredAttribute(parameter, "PathVariable", true);
    }

    /** 视为必填的校验注解简写名：NotNull、NotBlank、NotEmpty。 */
    private static final String[] REQUIRED_CONSTRAINT_ANNOTATIONS = {"NotNull", "NotBlank", "NotEmpty"};

    /**
     * 字段或参数是否带必填校验注解。
     * 认 NotNull、NotBlank、NotEmpty。
     */
    private boolean isRequiredField(PsiModifierListOwner element) {
        for (String name : REQUIRED_CONSTRAINT_ANNOTATIONS) {
            if (annotationResolver.hasAnnotation(element, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 PSI 类型构建 JSON Schema 节点（支持 object、array 及嵌套字段）。
     */
    private Map<String, Object> buildSchemaForType(PsiType type, TypeResolver.TypeMapping typeMapping) {
        return buildSchemaForType(type, typeMapping, new HashSet<>());
    }

    private Map<String, Object> buildSchemaForType(
            PsiType type,
            TypeResolver.TypeMapping typeMapping,
            Set<String> visiting) {
        if ("object".equals(typeMapping.getType())) {
            String typeKey = resolveSchemaTypeKey(type);
            if (typeKey != null && visiting.contains(typeKey)) {
                return Map.of("type", "object");
            }
        }

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", typeMapping.getType());
        if (typeMapping.getFormat() != null) {
            schema.put("format", typeMapping.getFormat());
        }

        if ("array".equals(typeMapping.getType())) {
            PsiType itemType = typeResolver.getCollectionElementType(type);
            if (itemType != null) {
                TypeResolver.TypeMapping itemMapping = typeResolver.resolveType(itemType);
                schema.put("items", buildSchemaForType(itemType, itemMapping, visiting));
            } else {
                schema.put("items", Map.of("type", "object"));
            }
            return schema;
        }

        if ("object".equals(typeMapping.getType()) && type instanceof PsiClassType classType) {
            PsiClass clazz = classType.resolve();
            if (clazz != null) {
                String typeKey = resolveSchemaTypeKey(type);
                if (typeKey != null) {
                    visiting.add(typeKey);
                }
                try {
                    Map<String, Object> nestedProperties = new HashMap<>();
                    List<String> nestedRequired = new ArrayList<>();
                    // 构建 JSON 请求体 schema 时，排除 static/transient/合成字段及 @JsonIgnore 字段
                    for (PsiField field : clazz.getAllFields()) {
                        if (!fieldExportFilter.shouldExportModelField(field)) {
                            continue;
                        }
                        TypeResolver.TypeMapping nestedMapping = typeResolver.resolveType(field.getType());
                        Map<String, Object> fieldSchema = buildSchemaForType(field.getType(), nestedMapping, visiting);

                        String description = annotationResolver.getSchemaAttribute(field, "description");
                        if (description == null || description.isEmpty()) {
                            description = javaDocResolver.getFieldDescription(field);
                        }
                        if (description != null && !description.isEmpty()) {
                            fieldSchema.put("description", description);
                        }

                        nestedProperties.put(field.getName(), fieldSchema);
                        if (isRequiredField(field)) {
                            nestedRequired.add(field.getName());
                        }
                    }
                    schema.put("properties", nestedProperties);
                    if (!nestedRequired.isEmpty()) {
                        schema.put("required", nestedRequired);
                    }
                } finally {
                    if (typeKey != null) {
                        visiting.remove(typeKey);
                    }
                }
            }
        }
        return schema;
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

    /**
     * 从 POJO 类型展开 form-data 或 urlencoded 字段列表。
     * 会跳过 {@code @JsonIgnore} 等不导出字段；urlencoded 模式下跳过文件类型字段；
     * multipart 下文件字段 {@code type=file}，文本等为类型解析结果（如 string）。
     *
     * @param type         表单对象类型
     * @param isUrlEncoded true 表示 application/x-www-form-urlencoded
     * @return 表单项列表
     */
    private List<RequestBody.FormDataItem> extractFormDataItems(PsiType type, boolean isUrlEncoded) {
        List<RequestBody.FormDataItem> items = new ArrayList<>();
        if (!(type instanceof PsiClassType classType)) {
            return items;
        }
        PsiClass clazz = classType.resolve();
        if (clazz == null) {
            return items;
        }
        for (PsiField field : clazz.getAllFields()) {
            if (!fieldExportFilter.shouldExportModelField(field)) {
                continue;
            }

            PsiType fieldType = field.getType();
            // urlencoded 只承载普通字段，跳过文件
            if (isUrlEncoded && isFileUploadType(fieldType)) {
                continue;
            }

            String description = annotationResolver.getSchemaAttribute(field, "description");
            if (description == null || description.isEmpty()) {
                description = javaDocResolver.getFieldDescription(field);
            }
            description = appendMultipleFileHint(description, fieldType);

            items.add(new RequestBody.FormDataItem(
                    field.getName(),
                    "",
                    resolveFormFieldType(fieldType, isUrlEncoded),
                    description != null ? description : "",
                    isRequiredField(field)
            ));
        }
        return items;
    }
}
