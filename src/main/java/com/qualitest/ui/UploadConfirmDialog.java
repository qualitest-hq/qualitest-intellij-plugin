package com.qualitest.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.qualitest.QualiTestBundle;
import com.qualitest.config.QualiTestSettings;
import com.qualitest.config.UploadConfigSupport;
import com.qualitest.scan.ApiUploadFilters;
import com.qualitest.scan.UploadScanStats;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.render.UiStyles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 项目级上传确认对话框（对齐 preview/upload-confirm-dialog.html）。
 */
public class UploadConfirmDialog extends QualiTestUploadDialog {

    private final List<ScannedApi> allApis;
    private final UploadScanStats stats;

    private JCheckBox onlyExplicitGroupCheckbox;

    public UploadConfirmDialog(@NotNull Project project, @NotNull List<ScannedApi> apis) {
        super(project);
        this.allApis = List.copyOf(apis);
        this.stats = ApiUploadFilters.summarize(allApis);

        setTitle(QualiTestBundle.message("upload.confirm.title"));
        setModal(true);
        setCancelButtonText(QualiTestBundle.message("dialog.button.cancel"));

        init();

        onlyExplicitGroupCheckbox.addActionListener(e -> updateUploadButtonText());
        updateUploadButtonText();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(4, 4, 6, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insetsBottom(14);

        JBLabel subtitle = new JBLabel(QualiTestBundle.message("upload.confirm.subtitle"));
        subtitle.setFont(UiStyles.DEFAULT_FONT.deriveFont(13f));
        subtitle.setForeground(UiStyles.TEXT_SECONDARY);
        panel.add(subtitle, gbc);

        gbc.gridy = 1;
        gbc.insets = JBUI.insetsBottom(18);
        UploadStatsPanel statsPanel = new UploadStatsPanel();
        statsPanel.updateStats(stats);
        panel.add(statsPanel, gbc);

        gbc.gridy = 2;
        gbc.insets = JBUI.emptyInsets();
        panel.add(createFilterBox(), gbc);

        return panel;
    }

    private JPanel createFilterBox() {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(true);
        box.setBackground(UiStyles.UPLOAD_FILTER_BG);
        box.setBorder(new CompoundBorder(
                new LineBorder(UiStyles.UPLOAD_BORDER_SOFT, 1, true),
                JBUI.Borders.empty(10, 10)
        ));

        QualiTestSettings settings = QualiTestSettings.getInstance();
        onlyExplicitGroupCheckbox = new JCheckBox();
        onlyExplicitGroupCheckbox.setOpaque(false);
        onlyExplicitGroupCheckbox.setFocusPainted(false);
        onlyExplicitGroupCheckbox.setSelected(settings != null && settings.isOnlyUploadExplicitGroup());

        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);

        JBLabel filterTitle = new JBLabel(QualiTestBundle.message("upload.filter.explicit.group"));
        filterTitle.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 13f));
        filterTitle.setForeground(UiStyles.TEXT_PRIMARY);
        filterTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        textColumn.add(filterTitle);

        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setOpaque(false);
        hintPanel.setBorder(JBUI.Borders.emptyTop(4));
        hintPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComponent filterHint = UploadUiText.createFilterHint();
        hintPanel.add(filterHint, BorderLayout.CENTER);
        textColumn.add(hintPanel);

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(onlyExplicitGroupCheckbox, BorderLayout.WEST);
        row.add(textColumn, BorderLayout.CENTER);

        MouseAdapter toggleOnClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == onlyExplicitGroupCheckbox) {
                    return;
                }
                onlyExplicitGroupCheckbox.doClick();
            }
        };
        row.addMouseListener(toggleOnClick);
        filterTitle.addMouseListener(toggleOnClick);
        hintPanel.addMouseListener(toggleOnClick);
        filterHint.addMouseListener(toggleOnClick);

        box.add(row, BorderLayout.CENTER);
        return box;
    }

    private void updateUploadButtonText() {
        boolean onlyExplicit = onlyExplicitGroupCheckbox.isSelected();
        int count = onlyExplicit ? stats.getExplicitApiCount() : stats.getTotalApiCount();
        String key = onlyExplicit ? "upload.confirm.button.tagged" : "upload.confirm.button.all";
        setOKButtonText(QualiTestBundle.message(key, count));
    }

    @Override
    public @NotNull Dimension getPreferredSize() {
        int width = JBUI.scale(UiStyles.UPLOAD_CONFIRM_WIDTH);
        return new Dimension(width, super.getPreferredSize().height);
    }

    @Override
    protected void doOKAction() {
        QualiTestSettings settings = QualiTestSettings.getInstance();
        if (settings == null || !UploadConfigSupport.ensureConfigured(project, settings)) {
            return;
        }

        boolean onlyExplicit = onlyExplicitGroupCheckbox.isSelected();
        settings.setOnlyUploadExplicitGroup(onlyExplicit);

        List<ScannedApi> toUpload = ApiUploadFilters.filterByExplicitGroup(allApis, onlyExplicit);
        if (toUpload.isEmpty()) {
            JOptionPane.showMessageDialog(
                    getContentPanel(),
                    QualiTestBundle.message("upload.filter.no.match"),
                    QualiTestBundle.message("prompt.title"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        close(OK_EXIT_CODE);
        // 项目级全量上传：鉴权配置为空时由服务端写入双端 Bearer 默认模板
        UploadTaskSupport.runUploadTask(
                project,
                settings.getServerUrl().trim(),
                settings.getProjectToken().trim(),
                toUpload,
                true
        );
    }
}
