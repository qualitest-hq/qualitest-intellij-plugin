package com.qualitest.ui;

import com.intellij.openapi.project.Project;
import com.qualitest.QualiTestBundle;
import com.qualitest.config.QualiTestSettings;
import com.qualitest.config.UploadConfigSupport;
import com.qualitest.scan.model.ScannedApi;

import javax.swing.*;
import java.awt.*;

/**
 * API 选择对话框。
 * 包装 ApiSelectionPanel 作为 Dialog 使用，支持选择待上传的 API。
 * 点击确定时检查上传配置（服务器地址与项目令牌），未配置则引导用户前往设置页面。
 */
public class ApiSelectionDialog extends QualiTestUploadDialog {

    private final ApiSelectionPanel selectionPanel;

    public ApiSelectionDialog(Project project, java.util.List<ScannedApi> apis) {
        super(project);
        this.selectionPanel = new ApiSelectionPanel(apis);

        setTitle(QualiTestBundle.message("dialog.title"));
        setModal(true);
        setOKButtonText(QualiTestBundle.message("dialog.button.upload"));
        setCancelButtonText(QualiTestBundle.message("dialog.button.cancel"));

        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return selectionPanel;
    }

    /**
     * 点击确定按钮时：检查上传配置是否完整，未配置则引导用户前往设置。
     */
    @Override
    protected void doOKAction() {
        QualiTestSettings settings = QualiTestSettings.getInstance();
        if (settings == null || !UploadConfigSupport.ensureConfigured(project, settings)) {
            return;
        }

        selectionPanel.performUpload(project, settings);
        super.doOKAction();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1200, 800);
    }
}
