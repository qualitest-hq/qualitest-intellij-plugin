package com.qualitest.scan.resolver;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JavaDoc解析器
 * 用于从JavaDoc注释中提取信息
 *
 * @author qualitest
 */
public class JavaDocResolver {

    private static final String DEFAULT_GROUP_TAG = "api.group";

    /** JavaDoc 块标签行（@param、@return、自定义标签等） */
    private static final Pattern BLOCK_TAG_LINE = Pattern.compile("^\\s*@\\w+.*");

    private final String groupTagName;

    public JavaDocResolver() {
        this(DEFAULT_GROUP_TAG);
    }

    /**
     * @param groupTagFormat 格式如 "api.group {group}"，其中 {group} 为占位符，
     *                       标签名 = @ 符号到 {group} 之间的内容（如 "api.group"）
     */
    public JavaDocResolver(String groupTagFormat) {
        this.groupTagName = tagNameFromFormat(groupTagFormat);
    }

    /**
     * 从「分组标签」配置串解析 {@code @} 后的标签名（如 {@code api.group}、{@code eo.groupName}）。
     */
    public static String tagNameFromFormat(@Nullable String format) {
        if (format == null || format.isBlank()) {
            return DEFAULT_GROUP_TAG;
        }
        String trimmed = format.trim();
        int placeholderIndex = trimmed.indexOf("{group}");
        if (placeholderIndex < 0) {
            // 未写 {group} 时仍允许只配置标签名，如 eo.groupName
            String t = trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
            return t.isEmpty() ? DEFAULT_GROUP_TAG : t;
        }
        String tagPart = trimmed.substring(0, placeholderIndex).trim();
        if (tagPart.isEmpty()) {
            return DEFAULT_GROUP_TAG;
        }
        return tagPart.startsWith("@") ? tagPart.substring(1) : tagPart;
    }

    /**
     * 从方法或类获取JavaDoc注释（只获取方法/类自己的注释）
     */
    public String getJavaDoc(PsiElement element) {
        if (element instanceof PsiMethod method) {
            PsiDocComment docComment = method.getDocComment();
            return docComment != null ? docComment.getText() : null;
        } else if (element instanceof PsiClass clazz) {
            PsiDocComment docComment = clazz.getDocComment();
            return docComment != null ? docComment.getText() : null;
        }
        return null;
    }

    /**
     * 获取JavaDoc描述（第一行）
     */
    public String getDescription(PsiElement element) {
        PsiDocComment docComment = null;

        if (element instanceof PsiMethod method) {
            docComment = method.getDocComment();
        } else if (element instanceof PsiClass clazz) {
            docComment = clazz.getDocComment();
        }

        return extractFirstSummaryLine(docComment);
    }

    /**
     * 字段声明处的说明：优先标准 JavaDoc；若无（例如仅用普通块注释或行注释），
     * 则读取紧邻字段上方的 PSI 注释，便于解析依赖库中实体、PageInfo 等字段注释。
     */
    public String getFieldDescription(PsiField field) {
        if (field == null) {
            return null;
        }
        String fromDoc = extractFirstSummaryLine(field.getDocComment());
        if (fromDoc != null && !fromDoc.isBlank()) {
            return sanitizeDocText(fromDoc);
        }
        return extractLeadingFieldComment(field);
    }

    /**
     * 紧邻字段之前的注释（跳过空白与字段上的注解列表）。
     */
    private String extractLeadingFieldComment(PsiField field) {
        PsiElement prev = field.getPrevSibling();
        while (prev instanceof PsiWhiteSpace) {
            prev = prev.getPrevSibling();
        }
        while (isAnnotationCluster(prev)) {
            prev = prev.getPrevSibling();
            while (prev instanceof PsiWhiteSpace) {
                prev = prev.getPrevSibling();
            }
        }
        if (prev instanceof PsiDocComment docComment) {
            return sanitizeDocText(extractFirstSummaryLine(docComment));
        }
        if (prev instanceof PsiComment) {
            return sanitizeDocText(normalizeJavaCommentToken(prev.getText()));
        }
        return null;
    }

    /** 是否为紧邻字段的注解或注解列表（部分 PSI 实现类名随版本变化，不用 PsiAnnotationList 硬依赖）。 */
    private static boolean isAnnotationCluster(PsiElement el) {
        if (el instanceof PsiAnnotation) {
            return true;
        }
        String simple = el != null ? el.getClass().getSimpleName() : "";
        return "PsiAnnotationList".equals(simple) || "LightPsiAnnotationList".equals(simple);
    }

    /**
     * 将行注释、块注释的原始文本转为单行说明。
     */
    private static String normalizeJavaCommentToken(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.startsWith("//")) {
            return t.substring(2).trim();
        }
        if (!t.startsWith("/*")) {
            return t.trim();
        }
        t = t.substring(2);
        if (t.endsWith("*/")) {
            t = t.substring(0, t.length() - 2);
        }
        StringBuilder sb = new StringBuilder();
        for (String line : t.split("\\R")) {
            String u = line.stripLeading();
            if (u.startsWith("*")) {
                u = u.substring(1).stripLeading();
            }
            if (!u.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(u);
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    /**
     * 从文档注释中提取摘要首行（排除纯星号装饰行）；若无描述元素则回退解析全文。
     */
    private String extractFirstSummaryLine(PsiDocComment docComment) {
        if (docComment == null) {
            return null;
        }

        PsiElement[] descriptionElements = docComment.getDescriptionElements();
        if (descriptionElements != null && descriptionElements.length > 0) {
            for (PsiElement descElement : descriptionElements) {
                String text = descElement.getText();
                if (text == null || text.trim().isEmpty()) {
                    continue;
                }
                String trimmed = text.trim();
                if (!isStarOnlyDecorationLine(trimmed)) {
                    return trimmed;
                }
            }
        }

        return firstSummaryLineFromNormalizedInner(docComment.getText());
    }

    /**
     * 获取方法 JavaDoc 中摘要第一行之后、任意块标签之前的正文。
     * <p>
     * 规则：去掉文档注释起止标记，每行去掉前导星号；跳过第一个非空行；
     * 收集后续非空行直至遇到块标签行；行之间用换行拼接。若无内容返回 null。
     */
    public String getApiDescriptionBody(PsiMethod method) {
        if (method == null) {
            return null;
        }
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null) {
            return null;
        }
        String raw = docComment.getText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String inner = extractInnerContent(raw);
        if (inner == null || inner.isBlank()) {
            return null;
        }

        String[] lines = inner.split("\\R");
        boolean skippedFirstNonEmpty = false;
        List<String> bodyLines = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = stripLeadingStars(line).trim();
            if (BLOCK_TAG_LINE.matcher(trimmedLine).matches()) {
                break;
            }
            if (trimmedLine.isEmpty()) {
                continue;
            }
            if (!skippedFirstNonEmpty) {
                skippedFirstNonEmpty = true;
                continue;
            }
            bodyLines.add(trimmedLine);
        }

        if (bodyLines.isEmpty()) {
            return null;
        }
        return String.join("\n", bodyLines);
    }

    private static String stripLeadingStars(String line) {
        if (line == null) {
            return "";
        }
        String s = line;
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        if (i < s.length() && s.charAt(i) == '*') {
            i++;
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
        return s.substring(i);
    }

    /**
     * 去掉块注释边界与装饰星号后返回正文；对缺少标准闭合或非标准开头的块注释做容错。
     */
    static String extractInnerContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (!s.startsWith("/**")) {
            return null;
        }
        s = s.substring(3);
        int endIdx = s.lastIndexOf("*/");
        if (endIdx >= 0) {
            s = s.substring(0, endIdx);
        }
        while (!s.isEmpty() && s.charAt(0) == '*') {
            s = s.substring(1);
        }
        String[] lines = s.split("\\R", -1);
        int start = 0;
        for (; start < lines.length; start++) {
            String t = stripLeadingStars(lines[start]).trim();
            if (t.isEmpty()) {
                continue;
            }
            if (isStarOnlyDecorationLine(t)) {
                continue;
            }
            break;
        }
        if (start >= lines.length) {
            return null;
        }
        return String.join("\n", Arrays.copyOfRange(lines, start, lines.length));
    }

    /**
     * 归一化后的 inner 中第一条非空、非「整行星号」行的正文。
     */
    private static String firstSummaryLineFromNormalizedInner(String raw) {
        String inner = extractInnerContent(raw);
        if (inner == null || inner.isBlank()) {
            return null;
        }
        for (String line : inner.split("\\R")) {
            String t = stripLeadingStars(line).trim();
            if (t.isEmpty()) {
                continue;
            }
            if (isStarOnlyDecorationLine(t)) {
                continue;
            }
            return t;
        }
        return null;
    }

    /** 整行在去掉边距后仅由 {@code *} 构成（如 {@code /***} 剥壳后首行只有一个星号）。 */
    static boolean isStarOnlyDecorationLine(String trimmedLine) {
        if (trimmedLine == null || trimmedLine.isEmpty()) {
            return false;
        }
        for (int i = 0; i < trimmedLine.length(); i++) {
            if (trimmedLine.charAt(i) != '*') {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取@return标签的值
     */
    public String getReturnDescription(PsiElement element) {
        PsiDocTag tag = findTag(element, "return");
        return tag != null ? extractTagValue(tag) : null;
    }

    /**
     * 获取@param标签的值
     */
    public String getParamDescription(PsiElement element, String paramName) {
        PsiDocComment docComment = getDocComment(element);
        if (docComment == null) {
            return null;
        }

        for (PsiDocTag tag : docComment.getTags()) {
            if ("param".equals(tag.getName())) {
                String value = tag.getValueElement() != null ? tag.getValueElement().getText() : null;
                if (paramName.equals(value)) {
                    return extractParamTagDescription(tag);
                }
            }
        }
        return null;
    }

    /**
     * 获取所有@param参数描述
     */
    public List<ParamDoc> getParamDocs(PsiElement element) {
        List<ParamDoc> params = new ArrayList<>();
        PsiDocComment docComment = getDocComment(element);
        if (docComment == null) {
            return params;
        }

        for (PsiDocTag tag : docComment.getTags()) {
            if ("param".equals(tag.getName())) {
                PsiDocTagValue valueElement = tag.getValueElement();
                if (valueElement != null) {
                    String name = valueElement.getText().trim();
                    String description = extractParamTagDescription(tag);
                    params.add(new ParamDoc(name, description));
                }
            }
        }
        return params;
    }

    /**
     * 检查是否有@deprecated标签
     */
    public boolean hasDeprecatedTag(PsiElement element) {
        return findTag(element, "deprecated") != null;
    }

    /**
     * 获取@deprecated标签的值
     */
    public String getDeprecatedDescription(PsiElement element) {
        PsiDocTag tag = findTag(element, "deprecated");
        return tag != null ? extractTagValue(tag) : null;
    }

    /**
     * 获取自定义分组标签的值
     */
    public String getApiGroup(PsiElement element) {
        PsiDocTag tag = findTag(element, groupTagName);
        return tag != null ? extractGroupTagValue(tag) : null;
    }

    /**
     * 获取元素的JavaDoc注释（只获取方法/类自己的注释，不从兄弟节点获取）
     */
    private PsiDocComment getDocComment(PsiElement element) {
        if (element instanceof PsiMethod method) {
            return method.getDocComment();
        } else if (element instanceof PsiClass clazz) {
            return clazz.getDocComment();
        }
        return null;
    }

    /**
     * 从 {@code @param} 标签取说明：在参数名 token 之后按文件内偏移截取，避免
     * {@link #extractTagValue} 把「首空格后整段」当作说明（会包含参数名重复、行尾边距 {@code *} 等）。
     */
    private String extractParamTagDescription(PsiDocTag tag) {
        PsiDocTagValue nameToken = tag.getValueElement();
        if (nameToken == null) {
            return sanitizeDocText(extractTagValue(tag));
        }
        PsiFile file = tag.getContainingFile();
        if (file == null) {
            return sanitizeDocText(extractTagValue(tag));
        }
        int from = nameToken.getTextRange().getEndOffset();
        int to = tag.getTextRange().getEndOffset();
        if (from >= to) {
            return null;
        }
        String raw = file.getText().substring(from, to);
        return sanitizeDocText(raw);
    }

    /**
     * 从自定义分组标签提取值：优先 PSI value token，再按文件偏移截取，最后回退 tag.getText()。
     */
    private String extractGroupTagValue(PsiDocTag tag) {
        PsiDocTagValue valueElement = tag.getValueElement();
        if (valueElement != null) {
            String fromValue = sanitizeGroupTagValue(valueElement.getText());
            if (fromValue != null && !fromValue.isBlank()) {
                return fromValue;
            }
        }

        PsiFile file = tag.getContainingFile();
        if (file != null) {
            String fromOffset = extractGroupTagValueByOffset(tag, file);
            if (fromOffset != null && !fromOffset.isBlank()) {
                return fromOffset;
            }
        }

        return sanitizeGroupTagValue(extractTagValueRaw(tag));
    }

    /**
     * 在源文件中定位 {@code @groupTagName} 后的分组名，截取到标签 text range 末尾。
     */
    private String extractGroupTagValueByOffset(PsiDocTag tag, PsiFile file) {
        String needle = "@" + groupTagName;
        String fileText = file.getText();
        int tagStart = tag.getTextRange().getStartOffset();
        int tagEnd = tag.getTextRange().getEndOffset();
        if (tagStart < 0 || tagEnd <= tagStart) {
            return null;
        }

        int atPos = fileText.indexOf(needle, tagStart);
        if (atPos < 0 || atPos >= tagEnd) {
            return null;
        }

        int valueStart = atPos + needle.length();
        while (valueStart < tagEnd) {
            char c = fileText.charAt(valueStart);
            if (c == ':' || Character.isWhitespace(c)) {
                valueStart++;
            } else {
                break;
            }
        }
        if (valueStart >= tagEnd) {
            return null;
        }

        return sanitizeGroupTagValue(fileText.substring(valueStart, tagEnd));
    }

    /**
     * 去掉 JavaDoc 块注释边距里粘在行尾的星号、注释闭合符以及多余空白。
     */
    static String sanitizeDocText(@Nullable String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.endsWith("*/")) {
            t = t.substring(0, t.length() - 2).trim();
        }

        StringBuilder sb = new StringBuilder();
        for (String line : t.split("\\R")) {
            String lineContent = stripLeadingStars(line).trim();
            lineContent = stripTrailingMarginStars(lineContent);
            if (!lineContent.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(lineContent);
            }
        }
        t = stripTrailingMarginStars(sb.toString().trim());
        t = t.replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 分组标签值清洗：只保留首行有效内容，并去除 Javadoc 边距星号。
     */
    static String sanitizeGroupTagValue(@Nullable String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.endsWith("*/")) {
            t = t.substring(0, t.length() - 2).trim();
        }

        for (String line : t.split("\\R")) {
            String lineContent = stripLeadingStars(line).trim();
            lineContent = stripTrailingMarginStars(lineContent);
            if (lineContent.isEmpty() || isStarOnlyDecorationLine(lineContent)) {
                continue;
            }
            if (BLOCK_TAG_LINE.matcher(lineContent).matches()) {
                break;
            }
            return sanitizeDocText(lineContent);
        }
        return null;
    }

    private static String stripTrailingMarginStars(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        while (end > 0 && line.charAt(end - 1) == '*') {
            end--;
        }
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end).trim();
    }

    /**
     * 从标签中提取值（去除标签名后的文本），不做清洗。
     */
    private String extractTagValueRaw(PsiDocTag tag) {
        String text = tag.getText();
        if (text == null) {
            return null;
        }

        int colonIndex = text.indexOf(':');
        if (colonIndex > 0 && colonIndex < text.length() - 1) {
            return text.substring(colonIndex + 1).trim();
        }
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < text.length() - 1) {
            return text.substring(spaceIndex + 1).trim();
        }
        return text.trim();
    }

    /**
     * 从标签中提取值（去除标签名后的文本）
     */
    private String extractTagValue(PsiDocTag tag) {
        return sanitizeDocText(extractTagValueRaw(tag));
    }

    private PsiDocTag findTag(PsiElement element, String tagName) {
        PsiDocComment docComment = getDocComment(element);
        if (docComment == null) {
            return null;
        }
        for (PsiDocTag tag : docComment.getTags()) {
            if (tagName.equals(tag.getName())) {
                return tag;
            }
        }
        // 自定义带点标签：部分环境下 tag.getName() 不可靠时，回退为原文中以 @tagName 开头的匹配
        String needle = "@" + tagName;
        for (PsiDocTag tag : docComment.getTags()) {
            String raw = tag.getText();
            if (raw == null) {
                continue;
            }
            int at = raw.indexOf('@');
            if (at < 0) {
                continue;
            }
            if (!raw.regionMatches(at, needle, 0, needle.length())) {
                continue;
            }
            int after = at + needle.length();
            if (after >= raw.length()) {
                return tag;
            }
            char c = raw.charAt(after);
            if (Character.isWhitespace(c) || c == '*') {
                return tag;
            }
        }
        return null;
    }

    /**
     * 参数文档
     */
    public static class ParamDoc {
        private final String name;
        private final String description;

        public ParamDoc(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
