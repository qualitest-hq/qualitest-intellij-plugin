package com.qualitest.scan.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.qualitest.scan.model.RequestConfig;
import com.qualitest.scan.model.ResponseConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.resolver.AnnotationResolver;
import com.qualitest.scan.resolver.FieldExportFilter;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.qualitest.scan.resolver.TypeResolver;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 验证响应 schema 与 Java 返回类型一致：R 包装保留 code/msg/data，裸 DTO 不再套外层 data 壳。
 */
public class ResponseConfigExtractorTest extends LightJavaCodeInsightFixtureTestCase {

    private ResponseConfigExtractor extractor;

    @Override
    protected String getTestDataPath() {
        Path path = Path.of("src/test/testdata/response");
        if (!path.toFile().isDirectory()) {
            path = Path.of("qualitest-intellij-plugin/src/test/testdata/response");
        }
        return path.toAbsolutePath().normalize().toString();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFiles(
                "com/qualitest/test/response/R.java",
                "com/qualitest/test/response/UserVO.java",
                "com/qualitest/test/response/TableDataInfo.java",
                "com/qualitest/test/response/SampleController.java"
        );
        AnnotationResolver annotationResolver = new AnnotationResolver();
        TypeResolver typeResolver = new TypeResolver();
        JavaDocResolver javaDocResolver = new JavaDocResolver();
        FieldExportFilter fieldExportFilter = new FieldExportFilter(annotationResolver, true);
        extractor = new ResponseConfigExtractor(annotationResolver, typeResolver, javaDocResolver, fieldExportFilter);
    }

    public void testUnifiedWrapper_keepsCodeMsgDataAtRoot() {
        Map<String, Object> schema = schemaForMethod("getR");
        Set<String> keys = propertyKeys(schema);
        assertTrue(keys.contains("code"));
        assertTrue(keys.contains("msg"));
        assertTrue(keys.contains("data"));
        assertFalse(isSingleDataWrapper(schema));
    }

    public void testBareDto_exposesFieldsAtRootWithoutDataWrapper() {
        Map<String, Object> schema = schemaForMethod("getBare");
        Set<String> keys = propertyKeys(schema);
        assertTrue(keys.contains("userId"));
        assertTrue(keys.contains("userName"));
        assertFalse(keys.contains("data"));
        assertFalse(isSingleDataWrapper(schema));
    }

    public void testTableDataInfo_exposesPaginationFieldsAtRoot() {
        Map<String, Object> schema = schemaForMethod("list");
        Set<String> keys = propertyKeys(schema);
        assertTrue(keys.contains("total"));
        assertTrue(keys.contains("rows"));
        assertTrue(keys.contains("code"));
        assertTrue(keys.contains("msg"));
        assertFalse(isSingleDataWrapper(schema));
    }

    public void testStringReturn_usesPrimitiveSchemaAtRoot() {
        Map<String, Object> schema = schemaForMethod("hello");
        assertEquals("string", schema.get("type"));
        assertFalse(schema.containsKey("properties"));
    }

    public void testArrayReturn_usesArraySchemaAtRoot() {
        Map<String, Object> schema = schemaForMethod("listUsers");
        assertEquals("array", schema.get("type"));
        assertNotNull(schema.get("items"));
        assertFalse(schema.containsKey("properties"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaForMethod(String methodName) {
        PsiClass controller = myFixture.findClass("com.qualitest.test.response.SampleController");
        PsiMethod method = Arrays.stream(controller.getMethods())
                .filter(m -> methodName.equals(m.getName()))
                .findFirst()
                .orElseThrow();
        ScannedApi api = ScannedApi.builder()
                .apiPath("/test/" + methodName)
                .requestConfig(RequestConfig.builder().method("GET").build())
                .build();
        extractor.extract(api, controller, method);
        ResponseConfig responseConfig = api.getResponseConfig();
        assertNotNull(responseConfig);
        assertNotNull(responseConfig.getResponses());
        assertFalse(responseConfig.getResponses().isEmpty());
        Map<String, Object> schema = responseConfig.getResponses().get(0).getSchema();
        assertNotNull(schema);
        return schema;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> propertyKeys(Map<String, Object> schema) {
        Object props = schema.get("properties");
        if (!(props instanceof Map<?, ?> map)) {
            return Set.of();
        }
        return ((Map<String, Object>) map).keySet();
    }

    private static boolean isSingleDataWrapper(Map<String, Object> schema) {
        Set<String> keys = propertyKeys(schema);
        return keys.size() == 1 && keys.contains("data");
    }
}
