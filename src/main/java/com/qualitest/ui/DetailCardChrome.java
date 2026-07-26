package com.qualitest.ui;

import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * 详情区通用卡片容器与分区标题。
 */
final class DetailCardChrome {

    JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiStyles.BG_CONTENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0x101010), 1, true),
                new EmptyBorder(10, 6, 8, 8)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.BOLD, 15));
        titleLabel.setForeground(UiStyles.TEXT_SECONDARY);
        titleLabel.setBorder(new EmptyBorder(0, 8, 4, 0));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));

        return card;
    }

    JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.DEFAULT_FONT);
        label.setForeground(UiStyles.PARAM_GREEN);
        label.setBorder(new EmptyBorder(0, 0, 2, 0));
        return label;
    }
}
