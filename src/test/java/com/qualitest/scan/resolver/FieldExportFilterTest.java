package com.qualitest.scan.resolver;

import com.qualitest.config.QualiTestSettings;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 验证「上传时排除 @JsonIgnore 字段」相关配置与过滤器的默认行为。
 * <p>
 * 业务背景：ApiParams 等查询 Bean 中，accountId 等由服务端注入的字段会标注
 * {@code @JsonIgnore}，插件默认不应把它们写入 queryParams。本测试覆盖设置项的
 * 默认值、空值兼容与用户显式关闭排除的场景。
 */
public class FieldExportFilterTest {

    /**
     * 全新安装或未迁移过的配置：State 默认 excludeJsonIgnoreFields=true，读取结果应为开启排除。
     */
    @Test
    public void settingsDefaultExcludesJsonIgnoreFields() {
        QualiTestSettings settings = new QualiTestSettings();
        settings.loadState(new QualiTestSettings.State());
        assertTrue(settings.isExcludeJsonIgnoreFields());
    }

    /**
     * 旧版持久化数据可能未写入该字段（null）：应视为开启排除，避免升级后突然上传 accountId 等字段。
     */
    @Test
    public void settingsNullExcludeJsonIgnoreMeansDefaultTrue() {
        QualiTestSettings.State state = new QualiTestSettings.State();
        state.excludeJsonIgnoreFields = null;
        QualiTestSettings settings = new QualiTestSettings();
        settings.loadState(state);
        assertTrue(settings.isExcludeJsonIgnoreFields());
    }

    /**
     * 用户在设置页取消勾选后，应允许带 @JsonIgnore 的字段参与上传。
     */
    @Test
    public void settingsCanDisableExclusion() {
        QualiTestSettings settings = new QualiTestSettings();
        settings.setExcludeJsonIgnoreFields(false);
        assertFalse(settings.isExcludeJsonIgnoreFields());
    }
}
