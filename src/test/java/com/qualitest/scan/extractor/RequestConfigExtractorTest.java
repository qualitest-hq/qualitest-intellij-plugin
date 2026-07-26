package com.qualitest.scan.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.qualitest.scan.model.ApiParameter;
import com.qualitest.scan.model.RequestBody;
import com.qualitest.scan.model.RequestConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.FieldExportFilter;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.resolver.TypeResolver;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link RequestConfigExtractor} 请求体扫描测试。
 * <p>
 * 覆盖：MultipartFile / RequestPart 扫成 form-data 的 file；
 * 文本表单项为 string；多文件 description 含 multiple；纯 JSON body 仍为 json。
 */
public class RequestConfigExtractorTest extends LightJavaCodeInsightFixtureTestCase {

    private RequestConfigExtractor extractor;

    /** 测试夹具根目录：Spring 注解 stub、MultipartFile stub、样例 Controller。 */
    @Override
    protected String getTestDataPath() {
        Path path = Path.of("src/test/testdata/request");
        if (!path.toFile().isDirectory()) {
            path = Path.of("qualitest-intellij-plugin/src/test/testdata/request");
        }
        return path.toAbsolutePath().normalize().toString();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // 加载真实包名下的 stub，供 PSI 解析注解与 MultipartFile 类型
        myFixture.configureByFiles(
                "org/springframework/web/multipart/MultipartFile.java",
                "org/springframework/web/bind/annotation/RequestPart.java",
                "org/springframework/web/bind/annotation/RequestParam.java",
                "org/springframework/web/bind/annotation/RequestBody.java",
                "org/springframework/web/bind/annotation/PostMapping.java",
                "org/springframework/web/bind/annotation/ModelAttribute.java",
                "com/qualitest/test/request/UploadForm.java",
                "com/qualitest/test/request/JsonBodyDto.java",
                "com/qualitest/test/request/FileUploadController.java"
        );
        AnnotationResolver annotationResolver = new AnnotationResolver();
        TypeResolver typeResolver = new TypeResolver();
        JavaDocResolver javaDocResolver = new JavaDocResolver();
        FieldExportFilter fieldExportFilter = new FieldExportFilter(annotationResolver, true);
        extractor = new RequestConfigExtractor(annotationResolver, typeResolver, javaDocResolver, fieldExportFilter);
    }

    /** @RequestPart MultipartFile + 文本 @RequestParam → form-data（file + string），不进 query。 */
    public void testRequestPartMultipartFile_mapsToFormDataFile() {
        RequestConfig config = configForMethod("uploadByRequestPart");
        assertEquals("form-data", config.getBody().getMode());

        Map<String, RequestBody.FormDataItem> form = formDataByName(config);
        assertEquals("file", form.get("file").getType());
        assertTrue(form.get("file").isRequired());
        assertEquals("string", form.get("bizType").getType());
        assertFalse(form.get("bizType").isRequired());

        assertFalse(queryHasName(config, "file"));
        assertFalse(queryHasName(config, "bizType"));
    }

    /** @RequestParam MultipartFile → form-data type=file，且不进 query。 */
    public void testRequestParamMultipartFile_mapsToFormDataFile() {
        RequestConfig config = configForMethod("uploadByRequestParam");
        assertEquals("form-data", config.getBody().getMode());

        Map<String, RequestBody.FormDataItem> form = formDataByName(config);
        assertNotNull(form.get("file"));
        assertEquals("file", form.get("file").getType());
        assertTrue(form.get("file").isRequired());
        assertFalse(queryHasName(config, "file"));
    }

    /** @ModelAttribute 含 MultipartFile 与 String 字段 → 展开为 file / string。 */
    public void testModelAttributeWithMultipartFile_expandsFormFields() {
        RequestConfig config = configForMethod("uploadByModelAttribute");
        assertEquals("form-data", config.getBody().getMode());

        Map<String, RequestBody.FormDataItem> form = formDataByName(config);
        assertEquals("file", form.get("file").getType());
        assertEquals("string", form.get("bizType").getType());
        assertFalse("text".equals(form.get("bizType").getType()));
        assertFalse(queryHasName(config, "file"));
        assertFalse(queryHasName(config, "bizType"));
    }

    /** consumes=multipart 的 @RequestBody DTO → 字段级 file / string。 */
    public void testRequestBodyMultipartDto_fieldTypesStringAndFile() {
        RequestConfig config = configForMethod("uploadByRequestBody");
        assertEquals("form-data", config.getBody().getMode());

        Map<String, RequestBody.FormDataItem> form = formDataByName(config);
        assertEquals("file", form.get("file").getType());
        assertEquals("string", form.get("bizType").getType());
    }

    /** List&lt;MultipartFile&gt; → type=file，描述含 multiple。 */
    public void testListMultipartFile_mapsToFileWithMultipleHint() {
        RequestConfig config = configForMethod("uploadList");
        assertEquals("form-data", config.getBody().getMode());

        Map<String, RequestBody.FormDataItem> form = formDataByName(config);
        RequestBody.FormDataItem files = form.get("files");
        assertNotNull(files);
        assertEquals("file", files.getType());
        assertTrue(files.getDescription().toLowerCase().contains("multiple"));
    }

    /** 普通 JSON @RequestBody 仍为 body.mode=json。 */
    public void testJsonBody_unchanged() {
        RequestConfig config = configForMethod("jsonBody");
        assertEquals("json", config.getBody().getMode());
        assertNotNull(config.getBody().getJson());
        assertNotNull(config.getBody().getJson().getSchema());
        assertEquals("object", config.getBody().getJson().getSchema().get("type"));
    }

    /** 对夹具 Controller 指定方法执行提取，返回请求配置。 */
    private RequestConfig configForMethod(String methodName) {
        PsiClass controller = myFixture.findClass("com.qualitest.test.request.FileUploadController");
        PsiMethod method = Arrays.stream(controller.getMethods())
                .filter(m -> methodName.equals(m.getName()))
                .findFirst()
                .orElseThrow();
        ScannedApi api = ScannedApi.builder()
                .apiPath("/test/" + methodName)
                .build();
        extractor.extract(api, controller, method);
        RequestConfig requestConfig = api.getRequestConfig();
        assertNotNull(requestConfig);
        assertNotNull(requestConfig.getBody());
        return requestConfig;
    }

    /** 将 formData 列表转为 name → 项 的映射，便于断言。 */
    private static Map<String, RequestBody.FormDataItem> formDataByName(RequestConfig config) {
        List<RequestBody.FormDataItem> formData = config.getBody().getFormData();
        assertNotNull(formData);
        assertFalse(formData.isEmpty());
        return formData.stream().collect(Collectors.toMap(
                RequestBody.FormDataItem::getName,
                Function.identity(),
                (a, b) -> a
        ));
    }

    /** queryParams 中是否存在指定名称的参数。 */
    private static boolean queryHasName(RequestConfig config, String name) {
        List<ApiParameter> queryParams = config.getQueryParams();
        if (queryParams == null) {
            return false;
        }
        return queryParams.stream().anyMatch(p -> name.equals(p.getName()));
    }
}
