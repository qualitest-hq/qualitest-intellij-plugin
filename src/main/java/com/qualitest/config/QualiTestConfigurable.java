package com.qualitest.config;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * QualiTest配置页面
 * 提供插件设置的用户界面
 *
 * @author qualitest
 */
public class QualiTestConfigurable implements Configurable {

    private QualiTestSettings settings;
    private QualiTestSettingsPanel panel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Qualitest Helper";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "qualitest-settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settings = QualiTestSettings.getInstance();
        if (settings == null) {
            JPanel error = new JPanel(new BorderLayout(0, 8));
            error.setBorder(JBUI.Borders.empty(12));
            error.add(new JBLabel(
                    "无法加载 QualiTest 设置服务。请确认插件已启用，并在 plugin.xml 中注册了 applicationService。"
            ), BorderLayout.NORTH);
            return error;
        }
        panel = new QualiTestSettingsPanel(settings);
        return panel.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settings == null || panel == null) {
            return false;
        }
        return panel.isModified(settings);
    }

    @Override
    public void apply() {
        if (panel != null && settings != null) {
            panel.applyTo(settings);
            // 分组标签变更后让 JavaDoc 等检查重新跑一遍，否则部分 IDE 版本会沿用旧标签列表需重启才消失
            ApplicationManager.getApplication().invokeLater(QualiTestConfigurable::restartDaemonInOpenProjects);
        }
    }

    private static void restartDaemonInOpenProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                DaemonCodeAnalyzer.getInstance(project).restart("qualitest.settings.groupTag");
            }
        }
    }

    @Override
    public void reset() {
        if (panel != null && settings != null) {
            panel.resetFrom(settings);
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        settings = null;
    }
}
