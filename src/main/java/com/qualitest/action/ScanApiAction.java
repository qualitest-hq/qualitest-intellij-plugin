package com.qualitest.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestNotifications;
import com.qualitest.UploadErrors;
import com.qualitest.scan.ApiScannerSupport;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.UploadConfirmDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 项目级上传：扫描整个项目中的 API，确认后批量上传。
 *
 * <p>索引未就绪（Dumb Mode）时通过 {@link DumbService#runWhenSmart} 延迟启动；
 * 实际扫描由 {@link com.qualitest.scan.ApiScanner} 的 {@code inSmartMode} 再次保证索引可用。
 */
public class ScanApiAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            QualiTestNotifications.showError(null, QualiTestBundle.message("error.project.missing"));
            return;
        }

        DumbService dumbService = DumbService.getInstance(project);
        Runnable startScan = () -> startScanBackgroundTask(project);
        if (dumbService.isDumb()) {
            QualiTestNotifications.showInfo(project,
                    QualiTestBundle.message("scan.wait.index.title"),
                    QualiTestBundle.message("scan.wait.index.detail"));
        }
        dumbService.runWhenSmart(startScan);
    }

    /**
     * 在后台线程执行全项目扫描；扫描本身通过 {@code inSmartMode} 再次保证索引已就绪。
     */
    private void startScanBackgroundTask(Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                QualiTestBundle.message("progress.scan.upload.title"), true) {
            private List<ScannedApi> apis;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    apis = ApiScannerSupport.forProject(project).scanProject(indicator);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (apis == null || apis.isEmpty()) {
                            QualiTestNotifications.showInfo(project,
                                    QualiTestBundle.message("scan.done.title"),
                                    QualiTestBundle.message("scan.done.no.apis"));
                            return;
                        }
                        new UploadConfirmDialog(project, apis).show();
                    });
                } catch (ProcessCanceledException ex) {
                    // 用户点击进度框「取消」，静默结束
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            QualiTestNotifications.showError(project,
                                    UploadErrors.formatNotification(ex)));
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}
