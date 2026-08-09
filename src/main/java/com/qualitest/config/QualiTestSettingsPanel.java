package com.qualitest.config;

import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestConstants;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qualitest.scan.resolver.AuthAnnotationMatcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * QualiTest设置面板
 * 设置页面的UI组件
 *
 * @author qualitest
 */
public class QualiTestSettingsPanel {

    private static final int FIELD_COLUMNS = 42;

    private JPanel panel;
    private JBTextField serverUrlField;
    private JBPasswordField projectTokenField;
    private JCheckBox scanDeprecatedCheckBox;
    /** 控制上传时是否跳过带 @JsonIgnore 的模型字段，默认勾选（排除） */
    private JCheckBox excludeJsonIgnoreFieldsCheckBox;
    private JCheckBox ignoreFirstGroupLevelCheckBox;
    private JBTextField groupTagField;
    /** 免登录注解输入框：每行一个短名或全限定名 */
    private JBTextArea anonymousAnnotationsArea;

    public QualiTestSettingsPanel(QualiTestSettings settings) {
        initComponents();
        resetFrom(settings);
    }

    private void initComponents() {
        serverUrlField = new JBTextField();
        projectTokenField = new JBPasswordField();
        scanDeprecatedCheckBox = new JCheckBox();
        // 勾选：accountId 等 @JsonIgnore 字段不出现在上传的 query/body/响应中；取消则全部导出
        excludeJsonIgnoreFieldsCheckBox = new JCheckBox();
        excludeJsonIgnoreFieldsCheckBox.setToolTipText(QualiTestBundle.message("settings.hint.exclude.json.ignore"));
        ignoreFirstGroupLevelCheckBox = new JCheckBox();
        ignoreFirstGroupLevelCheckBox.setToolTipText(QualiTestBundle.message("settings.ignore.first.group.level.hint"));
        groupTagField = new JBTextField();
        // 免登录注解名单：扫描时据此判断接口是否免登录
        anonymousAnnotationsArea = new JBTextArea(4, FIELD_COLUMNS);
        anonymousAnnotationsArea.setLineWrap(true);
        anonymousAnnotationsArea.setWrapStyleWord(true);
        anonymousAnnotationsArea.setToolTipText(QualiTestBundle.message("settings.hint.anonymous.annotations"));

        serverUrlField.setColumns(FIELD_COLUMNS);
        projectTokenField.setColumns(FIELD_COLUMNS);
        groupTagField.setColumns(FIELD_COLUMNS);

        JLabel groupHint = new JLabel(QualiTestBundle.message("settings.hint.group.tag"));
        groupHint.setForeground(UIUtil.getContextHelpForeground());
        JLabel anonymousHint = new JLabel(QualiTestBundle.message("settings.hint.anonymous.annotations"));
        anonymousHint.setForeground(UIUtil.getContextHelpForeground());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        int row = 0;

        Insets rowGap = JBUI.insets(0, 0, 8, 0);
        Insets labelGap = JBUI.insets(0, 0, 8, 8);

        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = JBUI.emptyInsets();
        form.add(new TitledSeparator("基础配置"), gc);
        row++;

        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.server.url"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(serverUrlField, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.project.token"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(projectTokenField, gc);
        row++;

        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = JBUI.insets(8, 0, 0, 0);
        form.add(new TitledSeparator("自定义配置"), gc);
        row++;

        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.scan.deprecated"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(scanDeprecatedCheckBox, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.exclude.json.ignore"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(excludeJsonIgnoreFieldsCheckBox, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.group.tag"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(groupTagField, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 1;
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.insets = JBUI.insets(2, 0, 8, 0);
        form.add(groupHint, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.ignore.first.group"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(ignoreFirstGroupLevelCheckBox, gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = labelGap;
        form.add(settingLabel("settings.label.anonymous.annotations"), gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.gridx = 1;
        gc.insets = rowGap;
        form.add(new JBScrollPane(anonymousAnnotationsArea), gc);
        row++;

        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.gridx = 1;
        gc.gridy = row;
        gc.insets = JBUI.insets(2, 0, 8, 0);
        form.add(anonymousHint, gc);
        row++;

        addBottomGlue(form, row);

        panel = form;
        panel.setBorder(new EmptyBorder(JBUI.insets(10, 12, 10, 12)));
    }

    /** 设置项左侧标签，文案来自 {@link QualiTestBundle}。 */
    private static JLabel settingLabel(String bundleKey) {
        return new JLabel(QualiTestBundle.message(bundleKey));
    }

    private static void addBottomGlue(JPanel form, int y) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 2;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), gc);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void resetFrom(QualiTestSettings settings) {
        if (settings == null) {
            serverUrlField.setText("");
            projectTokenField.setText("");
            scanDeprecatedCheckBox.setSelected(true);
            excludeJsonIgnoreFieldsCheckBox.setSelected(true);
            ignoreFirstGroupLevelCheckBox.setSelected(false);
            groupTagField.setText(QualiTestConstants.DEFAULT_GROUP_TAG);
            anonymousAnnotationsArea.setText(String.join("\n", QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS));
            return;
        }
        serverUrlField.setText(orEmpty(settings.getServerUrl()));
        projectTokenField.setText(orEmpty(settings.getProjectToken()));
        scanDeprecatedCheckBox.setSelected(settings.isScanDeprecated());
        excludeJsonIgnoreFieldsCheckBox.setSelected(settings.isExcludeJsonIgnoreFields());
        ignoreFirstGroupLevelCheckBox.setSelected(settings.isIgnoreFirstGroupLevel());
        groupTagField.setText(settings.getGroupTag());
        anonymousAnnotationsArea.setText(settings.getAnonymousAnnotationsText());
    }

    public void applyTo(QualiTestSettings settings) {
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setProjectToken(new String(projectTokenField.getPassword()));
        settings.setScanDeprecated(scanDeprecatedCheckBox.isSelected());
        settings.setExcludeJsonIgnoreFields(excludeJsonIgnoreFieldsCheckBox.isSelected());
        settings.setIgnoreFirstGroupLevel(ignoreFirstGroupLevelCheckBox.isSelected());
        settings.setGroupTag(groupTagField.getText().trim());
        settings.setAnonymousAnnotationsText(anonymousAnnotationsArea.getText());
    }

    public boolean isModified(QualiTestSettings settings) {
        if (!serverUrlField.getText().trim().equals(orEmpty(settings.getServerUrl()))) {
            return true;
        }

        if (!new String(projectTokenField.getPassword()).equals(orEmpty(settings.getProjectToken()))) {
            return true;
        }

        if (scanDeprecatedCheckBox.isSelected() != settings.isScanDeprecated()) {
            return true;
        }

        if (excludeJsonIgnoreFieldsCheckBox.isSelected() != settings.isExcludeJsonIgnoreFields()) {
            return true;
        }

        if (ignoreFirstGroupLevelCheckBox.isSelected() != settings.isIgnoreFirstGroupLevel()) {
            return true;
        }

        if (!groupTagField.getText().trim().equals(settings.getGroupTag())) {
            return true;
        }

        // 免登录注解：规范化后再比，避免空白/换行差异误判为已修改
        List<String> edited = AuthAnnotationMatcher.normalizeConfigured(List.of(anonymousAnnotationsArea.getText()));
        if (edited.isEmpty()) {
            edited = QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS;
        }
        return !edited.equals(settings.getAnonymousAnnotations());
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}
