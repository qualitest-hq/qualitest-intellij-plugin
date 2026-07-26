package com.qualitest.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.qualitest.ImportResultFormatter;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestIcons;
import com.qualitest.QualiTestNotifications;
import com.qualitest.UploadErrors;
import com.qualitest.config.QualiTestSettings;
import com.qualitest.config.UploadConfigSupport;
import com.qualitest.http.ApiClient;
import com.qualitest.scan.ImportResult;
import com.qualitest.scan.model.ScannedApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

/**
 * 在 Controller 上右键：直接将当前 Controller 的全部 API 上传，不打开选择对话框。
 */
public class ControllerUploadAllAction extends AnAction {

    public ControllerUploadAllAction() {
        super();
    }

    public ControllerUploadAllAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ControllerUploadSupport.Result resolved = ControllerUploadSupport.resolve(project, e);
        if (resolved.kind() == ControllerUploadSupport.Kind.NO_JAVA_FILE) {
            QualiTestNotifications.showError(project, QualiTestBundle.message("controller.error.no.java"));
            return;
        }
        if (resolved.kind() == ControllerUploadSupport.Kind.NO_CONTROLLER) {
            QualiTestNotifications.showError(project, QualiTestBundle.message("controller.error.not.controller"));
            return;
        }
        PsiClass controller = resolved.controller();
        if (controller == null) {
            return;
        }

        List<ScannedApi> apis = ControllerUploadSupport.scanController(project, controller);
        if (apis.isEmpty()) {
            QualiTestNotifications.showInfo(project,
                    QualiTestBundle.message("scan.done.title"),
                    QualiTestBundle.message("controller.scan.no.apis"));
            return;
        }

        QualiTestSettings settings = QualiTestSettings.getInstance();
        if (!UploadConfigSupport.ensureConfigured(project, settings)) {
            return;
        }

        runUploadTask(project, settings.getServerUrl().trim(), settings.getProjectToken().trim(), apis);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setIcon(QualiTestIcons.ACTION);
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        boolean visible = ControllerUploadSupport.resolve(project, e).kind() == ControllerUploadSupport.Kind.OK;
        e.getPresentation().setEnabledAndVisible(visible);
    }

    private void runUploadTask(Project project, String serverUrl, String projectToken, List<ScannedApi> apis) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                QualiTestBundle.message("progress.upload.title"), true) {
            private ImportResult result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(QualiTestBundle.message("progress.upload.apis.count", apis.size()));

                try {
                    ApiClient client = new ApiClient(serverUrl, projectToken);
                    result = client.uploadApis(apis);
                    indicator.setFraction(1.0);

                    ApplicationManager.getApplication().invokeLater(() -> showResult(project, result));
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            QualiTestNotifications.showError(project,
                                    UploadErrors.formatNotification(ex)));
                }
            }
        });
    }

    private void showResult(Project project, ImportResult result) {
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
