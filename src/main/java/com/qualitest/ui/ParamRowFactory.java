package com.qualitest.ui;

import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestStrings;
import com.qualitest.scan.model.RequestBody;
import com.qualitest.scan.model.ApiParameter;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * 参数列表单行（名称、类型、必填、说明）及表单行批量添加。
 */
final class ParamRowFactory {

    JPanel createParamRow(String name, String type, boolean required, String description) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(UiStyles.BG_CONTENT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 14f));
        nameLabel.setForeground(UiStyles.ACCENT_LIGHT_BLUE);
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel typeLabel = new JLabel(type != null ? type : "string");
        typeLabel.setFont(UiStyles.DEFAULT_FONT);
        typeLabel.setForeground(UiStyles.PARAM_GREEN);
        typeLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        typeLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel reqLabel = new JLabel(
                required ? QualiTestBundle.message("param.required") : QualiTestBundle.message("param.optional")
        );
        reqLabel.setFont(UiStyles.DEFAULT_FONT);
        reqLabel.setForeground(required ? UiStyles.METHOD_DELETE : UiStyles.PARAM_GREEN);
        reqLabel.setOpaque(true);
        reqLabel.setBackground(required ? UiStyles.REQUIRED_BG : UiStyles.OPTIONAL_BG);
        reqLabel.setBorder(new EmptyBorder(1, 6, 1, 6));
        reqLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        row.add(nameLabel);
        row.add(typeLabel);
        row.add(Box.createHorizontalStrut(10));
        row.add(reqLabel);

        String desc = QualiTestStrings.trimToNull(description);
        if (desc != null) {
            row.add(Box.createHorizontalStrut(10));
            String shown = DetailLabelText.formatInlineDescription(desc);
            JLabel descLabel = new JLabel(shown);
            descLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 13f));
            descLabel.setForeground(UiStyles.TEXT_SECONDARY);
            descLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            if (shown.endsWith("\u2026")) {
                descLabel.setToolTipText(DetailLabelText.tooltipHtmlForPlainText(desc));
            }
            row.add(descLabel);
        }

        row.setBorder(new EmptyBorder(1, 0, 1, 0));

        Dimension preferred = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return row;
    }

    void addParamRows(JPanel container, List<ApiParameter> params) {
        for (int i = 0; i < params.size(); i++) {
            ApiParameter param = params.get(i);
            container.add(createParamRow(param.getName(), param.getType(), param.isRequired(), param.getDescription()));
            if (i < params.size() - 1) {
                container.add(Box.createVerticalStrut(2));
            }
        }
    }

    void addFormDataRows(JPanel container, List<RequestBody.FormDataItem> items) {
        for (int i = 0; i < items.size(); i++) {
            RequestBody.FormDataItem item = items.get(i);
            container.add(createParamRow(item.getName(), item.getType(), item.isRequired(), item.getDescription()));
            if (i < items.size() - 1) {
                container.add(Box.createVerticalStrut(2));
            }
        }
    }

    JLabel createRequiredTag(boolean required) {
        JLabel reqLabel = new JLabel(
                required ? QualiTestBundle.message("param.required") : QualiTestBundle.message("param.optional")
        );
        reqLabel.setFont(UiStyles.DEFAULT_FONT);
        reqLabel.setForeground(required ? UiStyles.METHOD_DELETE : UiStyles.PARAM_GREEN);
        reqLabel.setOpaque(true);
        reqLabel.setBackground(required ? UiStyles.REQUIRED_BG : UiStyles.OPTIONAL_BG);
        reqLabel.setBorder(new EmptyBorder(1, 4, 1, 4));
        return reqLabel;
    }
}
