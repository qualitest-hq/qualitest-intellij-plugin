package com.qualitest;

import org.jetbrains.annotations.Nullable;

/**
 * 字符串小工具，避免在多处复制粘贴相同的修剪逻辑。
 */
public final class QualiTestStrings {

    private QualiTestStrings() {}

    /**
     * 空白串视为 null，便于配置校验。
     */
    @Nullable
    public static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
