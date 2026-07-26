package com.qualitest.scan;

import com.intellij.openapi.project.Project;
import com.qualitest.QualiTestConstants;
import com.qualitest.config.QualiTestSettings;

/**
 * 按当前设置构造 {@link ApiScanner}，避免在 Action 等处重复样板代码。
 */
public final class ApiScannerSupport {

    private ApiScannerSupport() {}

    public static ApiScanner forProject(Project project) {
        QualiTestSettings settings = QualiTestSettings.getInstance();
        String groupTag = settings != null ? settings.getGroupTag() : QualiTestConstants.DEFAULT_GROUP_TAG;
        boolean ignoreFirst = settings != null && settings.isIgnoreFirstGroupLevel();
        // 读取 @JsonIgnore 排除开关，传入扫描器供请求/响应字段过滤使用
        boolean excludeJsonIgnore = settings == null || settings.isExcludeJsonIgnoreFields();
        return new ApiScanner(project, groupTag, ignoreFirst, excludeJsonIgnore);
    }
}
