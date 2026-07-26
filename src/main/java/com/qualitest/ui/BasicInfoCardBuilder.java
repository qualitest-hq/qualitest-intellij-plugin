package com.qualitest.ui;

import com.qualitest.QualiTestBundle;import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 「基本信息」卡片：名称、方法、路径、分组，可选描述段落。
 */
final class BasicInfoCardBuilder {

    private final DetailCardChrome chrome;

    BasicInfoCardBuilder(DetailCardChrome chrome) {
        this.chrome = chrome;
    }

    JPanel createInfoCard(ScannedApi api) {
        JPanel card = chrome.createCard(QualiTestBundle.message("card.basic.info"));
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(UiStyles.BG_CONTENT);
        grid.setBorder(new EmptyBorder(4, 8, 4, 0));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 2, 8);

        String nameText = (api.getApiName() != null && !api.getApiName().isEmpty())
                ? api.getApiName()
                : nullToEmpty(api.getApiPath());
        String pathText = nullToEmpty(api.getApiPath());

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        grid.add(createBasicInfoLabel(QualiTestBundle.message("label.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        grid.add(createBasicInfoValue(nameText), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        grid.add(createBasicInfoLabel(QualiTestBundle.message("label.method")), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        grid.add(createMethodBadge(getMethodString(api)), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        grid.add(createBasicInfoLabel(QualiTestBundle.message("label.path")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        grid.add(createBasicInfoValue(pathText), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        grid.add(createBasicInfoLabel(QualiTestBundle.message("label.group")), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        grid.add(createBasicInfoValue(nullToEmpty(api.getApiGroup())), gbc);

        String apiDescription = api.getApiDescription();
        if (apiDescription != null && !apiDescription.isBlank()) {
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.NONE;
            grid.add(createBasicInfoSectionLabel(QualiTestBundle.message("label.description")), gbc);
            gbc.gridx = 1;
            gbc.gridwidth = 3;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            grid.add(createBasicInfoDetailArea(apiDescription.trim()), gbc);
        }

        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        card.add(grid);

        Dimension preferred = card.getPreferredSize();
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        card.setMinimumSize(new Dimension(0, preferred.height));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JLabel createBasicInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 14f));
        label.setForeground(new Color(0x8E8E8E));
        label.setPreferredSize(new Dimension(42, 22));
        return label;
    }

    private JLabel createBasicInfoSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 14f));
        label.setForeground(new Color(0x8E8E8E));
        return label;
    }

    /** 只读多行描述；宽度随容器伸缩，高度按换行后的内容计算。 */
    private JTextArea createBasicInfoDetailArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 15f));
        area.setForeground(new Color(0xD7D7D7));
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setSize(600, Short.MAX_VALUE);
        int contentHeight = Math.max(area.getPreferredSize().height, area.getFontMetrics(area.getFont()).getHeight());
        area.setPreferredSize(new Dimension(0, contentHeight));
        area.setMinimumSize(new Dimension(0, contentHeight));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, contentHeight));
        return area;
    }

    private JLabel createBasicInfoValue(String fullText) {
        String text = (fullText != null && !fullText.isEmpty()) ? fullText : "-";
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 15f));
        label.setForeground(new Color(0xD7D7D7));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        if (!"-".equals(text)) {
            label.setToolTipText(DetailLabelText.tooltipHtmlForPlainText(text));
        }
        return label;
    }

    private JLabel createMethodBadge(String method) {
        JLabel label = new JLabel(method);
        label.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.BOLD, 11f));
        label.setForeground(new Color(0x113530));
        label.setOpaque(true);
        label.setBackground(new Color(0x57D9C3));
        label.setBorder(new EmptyBorder(3, 10, 3, 10));
        return label;
    }

    private static String nullToEmpty(String str) {
        return str != null ? str : "-";
    }

    private static String getMethodString(ScannedApi api) {
        if (api.getRequestConfig() != null && api.getRequestConfig().getMethod() != null) {
            return api.getRequestConfig().getMethod().toUpperCase();
        }
        return "GET";
    }
}
