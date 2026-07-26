package com.qualitest;

import com.intellij.openapi.project.IndexNotReadyException;
import org.jetbrains.annotations.NotNull;

/**
 * 将上传异常格式化为用户可读的通知文案。
 */
public final class UploadErrors {

    private UploadErrors() {}

    @NotNull
    public static String formatNotification(@NotNull Throwable ex) {
        if (isIndexNotReady(ex)) {
            return QualiTestBundle.message("error.index.not.ready");
        }
        String detail = ex.getMessage();
        if (detail == null || detail.isBlank()) {
            return QualiTestBundle.message("error.upload.unknown");
        }
        return QualiTestBundle.message("message.upload.failed", detail);
    }

    private static boolean isIndexNotReady(@NotNull Throwable ex) {
        // 扫描链路可能包装异常，沿 cause 链查找 IndexNotReadyException
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof IndexNotReadyException) {
                return true;
            }
        }
        return false;
    }
}
