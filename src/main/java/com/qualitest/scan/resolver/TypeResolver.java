package com.qualitest.scan.resolver;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 类型解析器：将 Java PSI 类型映射为 JSON Schema 类型（type / format / items）。
 *
 * <p>解析顺序（{@link #resolveClassType}）：
 * <ol>
 *   <li>已知基础类型：按简单类名查表（含 {@code java.lang.*}、{@code java.math.*} 等）</li>
 *   <li>枚举 → {@code string}</li>
 *   <li>{@code Optional<T>} → 展开为 {@code T}</li>
 *   <li>日期时间：{@code java.time.*}、{@code java.sql.*}、{@code java.util.Date}</li>
 *   <li>集合 / Map：继承 {@code Collection} → {@code array}，继承 {@code Map} → {@code object}</li>
 *   <li>其余引用类型 → {@code object}（由上层提取器展开 POJO 字段）</li>
 * </ol>
 *
 * @author qualitest
 */
public class TypeResolver {

    /**
     * {@code java.util.Optional} 全限定名，用于泛型解包。
     */
    private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";

    /**
     * 简单类名 → Schema 映射表（不区分包名，如 {@code BigDecimal} 同时覆盖 {@code java.math.BigDecimal}）。
     */
    private static final Map<String, TypeMapping> PRIMITIVE_MAPPINGS = new HashMap<>();

    static {
        PRIMITIVE_MAPPINGS.put("String", new TypeMapping("string", null));
        PRIMITIVE_MAPPINGS.put("Integer", new TypeMapping("integer", "int32"));
        PRIMITIVE_MAPPINGS.put("int", new TypeMapping("integer", "int32"));
        PRIMITIVE_MAPPINGS.put("Long", new TypeMapping("integer", "int64"));
        PRIMITIVE_MAPPINGS.put("long", new TypeMapping("integer", "int64"));
        PRIMITIVE_MAPPINGS.put("Short", new TypeMapping("integer", "int32"));
        PRIMITIVE_MAPPINGS.put("short", new TypeMapping("integer", "int32"));
        PRIMITIVE_MAPPINGS.put("Byte", new TypeMapping("string", null));
        PRIMITIVE_MAPPINGS.put("Double", new TypeMapping("number", "double"));
        PRIMITIVE_MAPPINGS.put("double", new TypeMapping("number", "double"));
        PRIMITIVE_MAPPINGS.put("Float", new TypeMapping("number", "float"));
        PRIMITIVE_MAPPINGS.put("float", new TypeMapping("number", "float"));
        PRIMITIVE_MAPPINGS.put("BigDecimal", new TypeMapping("number", null));
        PRIMITIVE_MAPPINGS.put("BigInteger", new TypeMapping("integer", null));
        PRIMITIVE_MAPPINGS.put("Boolean", new TypeMapping("boolean", null));
        PRIMITIVE_MAPPINGS.put("boolean", new TypeMapping("boolean", null));
        PRIMITIVE_MAPPINGS.put("Character", new TypeMapping("string", null));
        PRIMITIVE_MAPPINGS.put("char", new TypeMapping("string", null));
        PRIMITIVE_MAPPINGS.put("UUID", new TypeMapping("string", "uuid"));
        PRIMITIVE_MAPPINGS.put("URI", new TypeMapping("string", "uri"));
        PRIMITIVE_MAPPINGS.put("URL", new TypeMapping("string", "uri"));
    }

    /**
     * 解析 {@link PsiType} 为 Schema 类型。
     *
     * @param psiType PSI 类型，null 时退化为 {@code string}
     * @return Schema 映射
     */
    public TypeMapping resolveType(PsiType psiType) {
        if (psiType == null) {
            return new TypeMapping("string", null);
        }

        // Java 数组（含基本类型 int[]、对象 String[][] 等），PSI 表示为 PsiArrayType，须单独映射为 JSON array
        if (psiType instanceof PsiArrayType arrayType) {
            return buildArrayType(arrayType.getComponentType());
        }

        if (psiType instanceof PsiPrimitiveType) {
            return getPrimitiveMapping(psiType.getPresentableText());
        }

        if (psiType instanceof PsiClassType classType) {
            return resolveClassType(classType);
        }

        return new TypeMapping("object", null);
    }

    /**
     * 解析引用类型（{@link PsiClassType}）。
     * 按「基础类型 → 枚举 → Optional → 日期 → 集合/Map → object」顺序分派。
     */
    private TypeMapping resolveClassType(PsiClassType classType) {
        String canonicalText = classType.getCanonicalText();

        TypeMapping knownType = lookupKnownType(canonicalText);
        if (knownType != null) {
            return knownType;
        }

        PsiClass resolved = classType.resolve();
        if (resolved == null) {
            return new TypeMapping("object", null);
        }

        if (resolved.isEnum()) {
            return new TypeMapping("string", null);
        }

        TypeMapping optionalMapping = resolveOptional(classType, resolved);
        if (optionalMapping != null) {
            return optionalMapping;
        }

        TypeMapping dateMapping = resolveDateTimeType(resolved);
        if (dateMapping != null) {
            return dateMapping;
        }

        TypeMapping collectionMapping = resolveCollectionType(classType, resolved);
        if (collectionMapping != null) {
            return collectionMapping;
        }

        return new TypeMapping("object", null);
    }

    /**
     * 按简单类名匹配已知基础类型（如 java.lang.String、java.math.BigDecimal、java.util.UUID）。
     */
    private TypeMapping lookupKnownType(String canonicalText) {
        return PRIMITIVE_MAPPINGS.get(getSimpleName(canonicalText));
    }

    /**
     * 解包 {@code Optional<T>}，返回内部类型映射；无泛型参数时退化为 {@code string}。
     */
    private TypeMapping resolveOptional(PsiClassType classType, PsiClass resolved) {
        if (!JAVA_UTIL_OPTIONAL.equals(resolved.getQualifiedName())) {
            return null;
        }
        PsiType[] parameters = classType.getParameters();
        if (parameters.length > 0) {
            return resolveType(parameters[0]);
        }
        return new TypeMapping("string", null);
    }

    /**
     * 解析日期时间类型。
     * {@code LocalDate}/{@code sql.Date} → format {@code date}；
     * {@code LocalTime}/{@code sql.Time} → format {@code time}；
     * 其余（含 {@code Timestamp}、{@code java.util.Date}）→ format {@code date-time}。
     *
     * @return 映射结果；非日期类型返回 {@code null}
     */
    private TypeMapping resolveDateTimeType(PsiClass resolved) {
        String qualifiedName = resolved.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }
        if (qualifiedName.startsWith("java.time.")) {
            String simpleName = getSimpleName(qualifiedName);
            if ("LocalDate".equals(simpleName)) {
                return new TypeMapping("string", "date");
            }
            if ("LocalTime".equals(simpleName)) {
                return new TypeMapping("string", "time");
            }
            return new TypeMapping("string", "date-time");
        }
        if ("java.sql.Date".equals(qualifiedName)) {
            return new TypeMapping("string", "date");
        }
        if ("java.sql.Time".equals(qualifiedName)) {
            return new TypeMapping("string", "time");
        }
        if ("java.sql.Timestamp".equals(qualifiedName)
                || InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_DATE)) {
            return new TypeMapping("string", "date-time");
        }
        return null;
    }

    /**
     * 解析集合与 Map。
     * 通过继承关系识别，因此 {@code ArrayList}、{@code HashSet} 等具体实现类均可正确映射为 {@code array}。
     *
     * @return 映射结果；非集合/Map 返回 {@code null}
     */
    private TypeMapping resolveCollectionType(PsiClassType classType, PsiClass resolved) {
        if (InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_MAP)) {
            return new TypeMapping("object", null);
        }
        if (InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            return buildArrayType(getCollectionElementType(classType));
        }
        return null;
    }

    /**
     * 统一数组映射创建入口，避免集合元素递归解析逻辑重复。
     */
    private TypeMapping buildArrayType(PsiType elementType) {
        if (elementType == null) {
            return new TypeMapping("array", null, null);
        }
        TypeMapping elementMapping = resolveType(elementType);
        return new TypeMapping("array", null, elementMapping);
    }

    /**
     * 获取类型名称（JSON Schema {@code type} 字段值）。
     */
    public String getTypeName(PsiType psiType) {
        return resolveType(psiType).getType();
    }

    /**
     * 获取格式（JSON Schema {@code format} 字段值，可能为 {@code null}）。
     */
    public String getFormat(PsiType psiType) {
        return resolveType(psiType).getFormat();
    }

    /**
     * 是否为集合或数组类型（不含 {@code Map}）。
     */
    public boolean isCollectionType(PsiType psiType) {
        if (psiType == null) {
            return false;
        }
        if (psiType instanceof PsiArrayType) {
            return true;
        }
        if (psiType instanceof PsiClassType classType) {
            PsiClass resolved = classType.resolve();
            return resolved != null
                    && InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_COLLECTION)
                    && !InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_MAP);
        }
        return false;
    }

    /**
     * 是否为 {@code Map} 及其子类/实现类。
     */
    public boolean isMapType(PsiType psiType) {
        if (psiType == null) {
            return false;
        }
        if (psiType instanceof PsiClassType classType) {
            PsiClass resolved = classType.resolve();
            return resolved != null && InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_UTIL_MAP);
        }
        return false;
    }

    /**
     * 获取集合或数组的元素类型（用于构建 JSON Schema 的 {@code items}）。
     *
     * @param collectionType 集合、数组或带泛型的容器类型
     * @return 元素类型；无法解析时返回 {@code null}
     */
    public PsiType getCollectionElementType(PsiType collectionType) {
        if (collectionType instanceof PsiArrayType arrayType) {
            return arrayType.getComponentType();
        }
        if (collectionType instanceof PsiClassType classType) {
            PsiType[] parameters = classType.getParameters();
            if (parameters.length > 0) {
                return parameters[0];
            }
        }
        return null;
    }

    /**
     * 获取类字段的PsiType
     */
    public PsiType getFieldType(PsiField field) {
        return field.getType();
    }

    /**
     * 获取 {@link PsiClass} 的简单类名（不含包名）。
     */
    public String getSimpleName(PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }
        String name = psiClass.getName();
        if (name != null) {
            return name;
        }
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName != null) {
            int lastDot = qualifiedName.lastIndexOf('.');
            return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        }
        return null;
    }

    /**
     * 基本类型（{@link PsiPrimitiveType}）映射；未知基本类型退化为 {@code string}。
     */
    private TypeMapping getPrimitiveMapping(String typeName) {
        return PRIMITIVE_MAPPINGS.getOrDefault(typeName, new TypeMapping("string", null));
    }

    /**
     * 从全限定名提取简单类名。
     */
    private String getSimpleName(String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * 类型映射结果，可递归转换为 JSON Schema Map。
     */
    public static class TypeMapping {
        /**
         * JSON Schema type：string / integer / number / boolean / array / object
         */
        private final String type;
        /**
         * JSON Schema format，如 int32、date-time；可为 null
         */
        private final String format;
        /**
         * 当 type 为 array 时，元素类型的映射
         */
        private final TypeMapping items;

        public TypeMapping(String type, String format) {
            this(type, format, null);
        }

        public TypeMapping(String type, String format, TypeMapping items) {
            this.type = type;
            this.format = format;
            this.items = items;
        }

        public String getType() {
            return type;
        }

        public String getFormat() {
            return format;
        }

        public TypeMapping getItems() {
            return items;
        }

        /**
         * 转为可序列化的 Schema Map（含嵌套 items）。
         */
        public Map<String, Object> toSchema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", type);
            if (format != null) {
                schema.put("format", format);
            }
            if (items != null) {
                schema.put("items", items.toSchema());
            }
            return schema;
        }
    }
}
