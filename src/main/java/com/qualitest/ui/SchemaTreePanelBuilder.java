package com.qualitest.ui;

import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 将 JSON Schema Map 渲染为纵向展开的简易树面板。
 */
final class SchemaTreePanelBuilder {

    private final ParamRowFactory paramRows;

    SchemaTreePanelBuilder(ParamRowFactory paramRows) {
        this.paramRows = paramRows;
    }

    JPanel createParamTree(Map<String, Object> schema) {
        JPanel tree = new JPanel();
        tree.setLayout(new BoxLayout(tree, BoxLayout.Y_AXIS));
        tree.setBackground(UiStyles.BG_CONTENT);
        tree.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (schema == null) return tree;

        if ("array".equals(schema.get("type")) && schema.get("items") instanceof Map<?, ?> items) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemSchema = (Map<String, Object>) items;
            appendSchemaPropertyRows(tree, extractSchemaProperties(itemSchema), extractRequiredFields(itemSchema));
            Dimension preferred = tree.getPreferredSize();
            tree.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
            return tree;
        }

        Map<String, Object> properties = extractSchemaProperties(schema);
        appendSchemaPropertyRows(tree, properties, extractRequiredFields(schema));
        Dimension preferred = tree.getPreferredSize();
        tree.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return tree;
    }

    private void appendSchemaPropertyRows(JPanel tree, Map<String, Object> properties, Set<String> requiredFields) {
        int index = 0;
        int size = properties.size();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            tree.add(createTreeItem(entry.getKey(), entry.getValue(), requiredFields.contains(entry.getKey())));
            if (index < size - 1) {
                tree.add(Box.createVerticalStrut(2));
            }
            index++;
        }
    }

    private JPanel createTreeItem(String name, Object value) {
        return createTreeItem(name, value, false);
    }

    private JPanel createTreeItem(String name, Object value, boolean required) {
        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBackground(UiStyles.BG_CONTENT);
        item.setBorder(new EmptyBorder(1, 16, 1, 0));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(UiStyles.BG_CONTENT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent branchGlyph = createTreeBranchGlyph();

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 14f));
        nameLabel.setForeground(UiStyles.ACCENT_LIGHT_BLUE);
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        String typeStr = resolveTypeLabel(value);

        JLabel typeLabel = new JLabel(typeStr);
        typeLabel.setFont(UiStyles.DEFAULT_FONT);
        typeLabel.setForeground(UiStyles.PARAM_GREEN);
        typeLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        typeLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        String schemaDescription = extractSchemaDescription(value);

        row.add(branchGlyph);
        row.add(nameLabel);
        row.add(typeLabel);
        row.add(Box.createHorizontalStrut(6));
        row.add(paramRows.createRequiredTag(required));
        if (schemaDescription != null) {
            row.add(Box.createHorizontalStrut(8));
            String shown = DetailLabelText.formatInlineDescription(schemaDescription);
            JLabel descLabel = new JLabel(shown);
            descLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.PLAIN, 13f));
            descLabel.setForeground(UiStyles.TEXT_SECONDARY);
            descLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            if (shown.endsWith("\u2026")) {
                descLabel.setToolTipText(DetailLabelText.tooltipHtmlForPlainText(schemaDescription.trim()));
            }
            row.add(descLabel);
        }
        item.add(row);
        Dimension rowPreferred = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowPreferred.height));

        if (value instanceof Map<?, ?> mapValue) {
            Object props = mapValue.get("properties");
            if (props instanceof Map<?, ?> properties && !properties.isEmpty()) {
                Set<String> reqFields = extractRequiredFields(mapValue);
                JPanel children = new JPanel();
                children.setLayout(new BoxLayout(children, BoxLayout.Y_AXIS));
                children.setBackground(UiStyles.BG_CONTENT);
                children.setAlignmentX(Component.LEFT_ALIGNMENT);
                children.setBorder(new EmptyBorder(0, 12, 0, 0));

                int idx = 0;
                int sz = properties.size();
                for (Map.Entry<?, ?> propEntry : properties.entrySet()) {
                    Object nodeValue = propEntry.getValue();
                    boolean childRequired = reqFields.contains(propEntry.getKey().toString());
                    children.add(createTreeItem(propEntry.getKey().toString(), nodeValue, childRequired));
                    if (idx < sz - 1) {
                        children.add(Box.createVerticalStrut(2));
                    }
                    idx++;
                }
                Dimension childrenPreferred = children.getPreferredSize();
                children.setMaximumSize(new Dimension(Integer.MAX_VALUE, childrenPreferred.height));
                item.add(children);
            } else if ("array".equals(mapValue.get("type")) && mapValue.get("items") instanceof Map<?, ?> itemSchema) {
                JPanel children = new JPanel();
                children.setLayout(new BoxLayout(children, BoxLayout.Y_AXIS));
                children.setBackground(UiStyles.BG_CONTENT);
                children.setAlignmentX(Component.LEFT_ALIGNMENT);
                children.setBorder(new EmptyBorder(0, 12, 0, 0));
                children.add(createTreeItem("items", itemSchema));
                Dimension childrenPreferred = children.getPreferredSize();
                children.setMaximumSize(new Dimension(Integer.MAX_VALUE, childrenPreferred.height));
                item.add(children);
            }
        }

        Dimension itemPreferred = item.getPreferredSize();
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, itemPreferred.height));
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSchemaProperties(Map<String, Object> schema) {
        Object properties = schema.get("properties");
        if (properties instanceof Map<?, ?> propertiesMap) {
            return (Map<String, Object>) propertiesMap;
        }
        return Map.of();
    }

    private Set<String> extractRequiredFields(Map<?, ?> schema) {
        Set<String> requiredFields = new HashSet<>();
        Object required = schema.get("required");
        if (required instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    requiredFields.add(item.toString());
                }
            }
        }
        return requiredFields;
    }

    private String resolveTypeLabel(Object value) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return "object";
        }
        Object type = mapValue.get("type");
        String typeName = type != null ? type.toString() : "object";
        Object format = mapValue.get("format");
        if (format != null && !format.toString().isEmpty()) {
            return typeName + "(" + format + ")";
        }
        return typeName;
    }

    /** 读取 Schema 节点上的 {@code description} 字段。 */
    private static String extractSchemaDescription(Object value) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return null;
        }
        Object d = mapValue.get("description");
        if (d == null) {
            return null;
        }
        String s = d.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private JComponent createTreeBranchGlyph() {
        JPanel glyph = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x5A5A5A));
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int x = 6;
                    int yMid = (getHeight() / 2) + 1;
                    g2.drawLine(x, 3, x, getHeight() - 1);
                    g2.drawLine(x, yMid, getWidth() - 2, yMid);
                } finally {
                    g2.dispose();
                }
            }
        };
        glyph.setOpaque(false);
        glyph.setAlignmentY(Component.CENTER_ALIGNMENT);
        Dimension size = new Dimension(16, 14);
        glyph.setPreferredSize(size);
        glyph.setMinimumSize(size);
        glyph.setMaximumSize(size);
        return glyph;
    }
}
