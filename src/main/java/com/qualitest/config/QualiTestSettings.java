package com.qualitest.config;

import com.qualitest.QualiTestConstants;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qualitest.scan.resolver.AuthAnnotationMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * QualiTest设置数据类
 * 存储插件配置信息
 *
 * @author qualitest
 */
@State(name = "QualiTestSettings", storages = @Storage("qualitest-settings.xml"))
public class QualiTestSettings implements PersistentStateComponent<QualiTestSettings.State> {

    private State state = new State();

    public static QualiTestSettings getInstance() {
        return ApplicationManager.getApplication().getService(QualiTestSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        if (this.state.serverUrl == null) {
            this.state.serverUrl = "http://localhost:8080";
        }
        if (this.state.projectToken == null) {
            this.state.projectToken = "";
        }
        if (this.state.groupTag == null || this.state.groupTag.isBlank()) {
            this.state.groupTag = QualiTestConstants.DEFAULT_GROUP_TAG;
        }
        // 旧配置可能没有该字段，补上默认免登录注解名单
        if (this.state.anonymousAnnotations == null) {
            this.state.anonymousAnnotations = new ArrayList<>(QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS);
        }
    }

    public String getServerUrl() {
        return state.serverUrl != null ? state.serverUrl : "";
    }

    public void setServerUrl(String url) {
        state.serverUrl = url;
    }

    public String getProjectToken() {
        return state.projectToken != null ? state.projectToken : "";
    }

    public void setProjectToken(String token) {
        state.projectToken = token;
    }

    public boolean isScanDeprecated() {
        return state.scanDeprecated;
    }

    public void setScanDeprecated(boolean scan) {
        state.scanDeprecated = scan;
    }

    public String getGroupTag() {
        if (state.groupTag == null || state.groupTag.isBlank()) {
            return QualiTestConstants.DEFAULT_GROUP_TAG;
        }
        return state.groupTag.trim();
    }

    public void setGroupTag(String groupTag) {
        if (groupTag == null || groupTag.isBlank()) {
            state.groupTag = QualiTestConstants.DEFAULT_GROUP_TAG;
        } else {
            state.groupTag = groupTag.trim();
        }
    }

    /** 为 true 时，分组字符串去掉首个「.」及其左侧（如其它插件写的模块前缀） */
    public boolean isIgnoreFirstGroupLevel() {
        return state.ignoreFirstGroupLevel;
    }

    public void setIgnoreFirstGroupLevel(boolean ignoreFirstGroupLevel) {
        state.ignoreFirstGroupLevel = ignoreFirstGroupLevel;
    }

    /** 上传时是否默认仅上传带分组注释的 Controller */
    public boolean isOnlyUploadExplicitGroup() {
        return state.onlyUploadExplicitGroup;
    }

    public void setOnlyUploadExplicitGroup(boolean onlyUploadExplicitGroup) {
        state.onlyUploadExplicitGroup = onlyUploadExplicitGroup;
    }

    /**
     * 上传接口文档时是否排除带 {@code @JsonIgnore} 的模型字段。
     * <p>
     * 默认开启：query 参数、JSON 请求体、表单字段、响应 schema 均不导出这类字段。
     * 关闭后仍会扫描并上传带该注解的字段（例如需要完整对照源码时）。
     */
    public boolean isExcludeJsonIgnoreFields() {
        return state.excludeJsonIgnoreFields == null || state.excludeJsonIgnoreFields;
    }

    /** 写入「上传时排除 @JsonIgnore 字段」开关，由设置面板调用 */
    public void setExcludeJsonIgnoreFields(boolean excludeJsonIgnoreFields) {
        state.excludeJsonIgnoreFields = excludeJsonIgnoreFields;
    }

    /**
     * 免登录注解列表（短名或全限定名）。
     * 扫描时：方法或类命中名单中任一注解 → 接口标为免登录；名单为空时回退默认值。
     */
    @NotNull
    public List<String> getAnonymousAnnotations() {
        List<String> normalized = AuthAnnotationMatcher.normalizeConfigured(state.anonymousAnnotations);
        if (normalized.isEmpty()) {
            return QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS;
        }
        return normalized;
    }

    /**
     * 写入免登录注解名单；空列表会回退为默认值。
     */
    public void setAnonymousAnnotations(List<String> annotations) {
        List<String> normalized = AuthAnnotationMatcher.normalizeConfigured(annotations);
        state.anonymousAnnotations = new ArrayList<>(
                normalized.isEmpty() ? QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS : normalized);
    }

    /**
     * 设置页多行文本形式的免登录注解（每行一个，也可用逗号分隔）。
     */
    @NotNull
    public String getAnonymousAnnotationsText() {
        return String.join("\n", getAnonymousAnnotations());
    }

    /**
     * 从设置页多行文本写入免登录注解名单。
     */
    public void setAnonymousAnnotationsText(String text) {
        if (text == null || text.isBlank()) {
            setAnonymousAnnotations(QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS);
            return;
        }
        // 整段交给规范化逻辑按行/逗号拆分
        setAnonymousAnnotations(List.of(text));
    }

    /**
     * 配置状态
     */
    public static class State {
        public String serverUrl = "http://localhost:8080";
        public String projectToken = "";
        public boolean scanDeprecated = true;
        public String groupTag = QualiTestConstants.DEFAULT_GROUP_TAG;
        public boolean ignoreFirstGroupLevel = false;
        public boolean onlyUploadExplicitGroup = false;
        /**
         * 上传时排除 {@code @JsonIgnore} 字段。
         * 使用 Boolean 以便区分「用户从未配置」与「显式关闭」：null 与 true 均视为开启排除。
         */
        public Boolean excludeJsonIgnoreFields = true;
        /**
         * 免登录注解短名或全限定名列表。
         * null 表示尚未配置，读取时按默认名单处理。
         */
        public List<String> anonymousAnnotations = new ArrayList<>(QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS);
    }
}
