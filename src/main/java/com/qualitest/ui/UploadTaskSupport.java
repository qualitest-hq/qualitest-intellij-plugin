package com.qualitest.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.qualitest.ImportResultFormatter;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestNotifications;
import com.qualitest.UploadErrors;
import com.qualitest.http.ApiClient;
import com.qualitest.scan.ImportResult;
import com.qualitest.scan.model.ScannedApi;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 后台上传任务与结果通知的共用逻辑。
 */
public final class UploadTaskSupport {

    private UploadTaskSupport() {}

    /**
     * 后台上传（默认不触发项目鉴权种子，适合单 Controller / 勾选上传）。
     */
    public static void runUploadTask(
            @NotNull Project project,
            @NotNull String serverUrl,
            @NotNull String projectToken,
            @NotNull List<ScannedApi> apis
    ) {
        runUploadTask(project, serverUrl, projectToken, apis, false);
    }

    /**
     * @param seedProjectAuthIfEmpty 项目级全量上传传 true，使服务端在鉴权配置为空时写入默认模板
     */
    public static void runUploadTask(
            @NotNull Project project,
            @NotNull String serverUrl,
            @NotNull String projectToken,
            @NotNull List<ScannedApi> apis,
            boolean seedProjectAuthIfEmpty
    ) {
        final int count = apis.size();
        ProgressManager.getInstance().run(new Task.Backgroundable(
                project, QualiTestBundle.message("progress.upload.title"), true) {
            private ImportResult result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(QualiTestBundle.message("progress.upload.apis.count", count));

                try {
                    ApiClient client = new ApiClient(serverUrl, projectToken);
                    result = client.uploadApis(apis, seedProjectAuthIfEmpty);
                    indicator.setFraction(1.0);

                    ApplicationManager.getApplication()
                            .invokeLater(() -> showUploadResult(project, result));
                } catch (Exception ex) {
                    ApplicationManager.getApplication()
                            .invokeLater(() -> QualiTestNotifications.showError(project,
                                    UploadErrors.formatNotification(ex)));
                }
            }
        });
    }

    private static void showUploadResult(@NotNull Project project, ImportResult result) {
        if (result == null) {
            QualiTestNotifications.showError(project, QualiTestBundle.message("upload.failed.generic"));
            return;
        }
        String message = ImportResultFormatter.formatSummaryMultiline(result);
        if (result.isSuccess()) {
            QualiTestNotifications.showInfo(project, QualiTestBundle.message("import.success.title"), message);
        } else {
            QualiTestNotifications.showInfo(project, QualiTestBundle.message("import.done.title"), message);
        }
    }
}
