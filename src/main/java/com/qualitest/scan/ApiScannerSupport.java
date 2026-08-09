package com.qualitest.scan;

import com.intellij.openapi.project.Project;
import com.qualitest.QualiTestConstants;
import com.qualitest.config.QualiTestSettings;

/**
 * 按当前插件设置创建扫描器。
 * <p>
 * 统一读取分组标签、是否忽略首级分组、是否排除 JsonIgnore 字段、免登录注解名单，
 * 避免各 Action 里重复拼装构造参数。
 */
public final class ApiScannerSupport {

    private ApiScannerSupport() {}

    /**
     * 用应用级设置构造面向指定 IDEA 工程的扫描器。
     */
    public static ApiScanner forProject(Project project) {
        QualiTestSettings settings = QualiTestSettings.getInstance();
        String groupTag = settings != null ? settings.getGroupTag() : QualiTestConstants.DEFAULT_GROUP_TAG;
        boolean ignoreFirst = settings != null && settings.isIgnoreFirstGroupLevel();
        // 是否在上传文档时丢掉带 @JsonIgnore 的模型字段
        boolean excludeJsonIgnore = settings == null || settings.isExcludeJsonIgnoreFields();
        // 免登录注解名单，用于给接口打 auth 标签
        var anonymousAnnotations = settings != null
                ? settings.getAnonymousAnnotations()
                : QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS;
        return new ApiScanner(project, groupTag, ignoreFirst, excludeJsonIgnore, anonymousAnnotations);
    }
}
