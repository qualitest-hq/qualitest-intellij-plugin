package com.qualitest.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestIcons;
import com.qualitest.QualiTestNotifications;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.ApiSelectionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

/**
 * 在 Controller 上右键：打开选择面板，勾选后再上传。
 */
public class ControllerSelectUploadAction extends AnAction {

    public ControllerSelectUploadAction() {
        super();
    }

    public ControllerSelectUploadAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
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

        new ApiSelectionDialog(project, apis).show();
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
}
