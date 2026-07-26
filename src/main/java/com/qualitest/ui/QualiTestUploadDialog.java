package com.qualitest.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.qualitest.QualiTestBundle;
import com.qualitest.config.UploadConfigSupport;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * 上传相关对话框基类：左下角提供「设置」入口。
 */
abstract class QualiTestUploadDialog extends DialogWrapper {

    protected final Project project;

    protected QualiTestUploadDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;
    }

    @Override
    protected Action @NotNull [] createLeftSideActions() {
        return new Action[]{
                new DialogWrapperAction(QualiTestBundle.message("dialog.button.settings")) {
                    @Override
                    protected void doAction(ActionEvent e) {
                        UploadConfigSupport.openSettings(project);
                    }
                }
        };
    }
}
