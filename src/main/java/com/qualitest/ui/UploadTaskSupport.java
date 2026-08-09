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
import com.qualitest.scan.model.ApiImportUploadType;
import com.qualitest.scan.model.ScannedApi;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 后台上传任务与结果通知的共用逻辑。
 * <p>
 * 三种入口（项目级 / Controller 全部 / Controller 选择）共用，须传 {@link ApiImportUploadType}。
 */
public final class UploadTaskSupport {

    private UploadTaskSupport() {}

    /**
     * 后台上传；由 {@code uploadType} 告知服务端入口类型（如项目级可种子空鉴权配置）。
     */
    public static void runUploadTask(
            @NotNull Project project,
            @NotNull String serverUrl,
            @NotNull String projectToken,
            @NotNull List<ScannedApi> apis,
            @NotNull ApiImportUploadType uploadType
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
                    result = client.uploadApis(apis, uploadType);
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
