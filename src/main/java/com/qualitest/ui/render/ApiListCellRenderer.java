package com.qualitest.ui.render;

import com.qualitest.scan.model.ScannedApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * API 列表项渲染器。
 * 为 {@link JList} 中的每个 {@link ScannedApi} 渲染一行：
 * [METHOD] [API 名称/路径]    [复选框]
 *
 * <p>颜色主题遵循 IntelliJ Darcula 暗色风格，HTTP 方法使用语义化配色
 * （GET=青、POST=蓝、DELETE=红、PATCH=紫）。
 *
 * <p>渲染器通过 JList 的 ClientProperty "selectedIndices" 读取外部选中状态，
 * 无需持有 Panel 的内部引用，实现解耦。
 *
 * @author qualitest
 */
public class ApiListCellRenderer extends JPanel implements ListCellRenderer<ScannedApi> {

    /** 左侧列表固定宽度见 {@link UiStyles#LIST_PANEL_WIDTH}；预留方法徽章、间距、复选框与内边距后的文本区上限估算 */
    private static final int SIDEBAR_RESERVED_FOR_BADGE_STRUT_CHECKBOX_PX = 130;

    private final JLabel methodLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JCheckBox checkBox = new JCheckBox();
    private final EmptyBorder emptyBorder = new EmptyBorder(8, 12, 8, 12);

    public ApiListCellRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBackground(UiStyles.BG_CONTENT);
        setBorder(emptyBorder);
        setOpaque(true);

        // HTTP 方法标签：固定宽度、等宽字体、语义配色
        methodLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.BOLD, 10));
        methodLabel.setOpaque(true);
        methodLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        methodLabel.setHorizontalAlignment(SwingConstants.CENTER);
        methodLabel.setPreferredSize(new Dimension(52, 20));

        // API 名称标签
        nameLabel.setFont(UiStyles.DEFAULT_FONT);
        nameLabel.setForeground(UiStyles.TEXT_NORMAL);

        // 复选框：选中态用于"待上传"标记
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        checkBox.setBorderPainted(false);
        checkBox.setContentAreaFilled(false);

        // 左侧：方法标签 + 名称
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(methodLabel);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(nameLabel);

        add(leftPanel, BorderLayout.CENTER);
        add(checkBox, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ScannedApi> list,
            ScannedApi api,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        String method = getMethod(api);
        methodLabel.setText(method);
        methodLabel.setBackground(UiStyles.getMethodColor(method));
        methodLabel.setForeground(UiStyles.getMethodTextColor(method));

        // API 显示名称：优先使用 apiName，回退为路径；过长则在侧栏用省略号，悬浮显示全文
        String apiName = api.getApiName();
        if (apiName == null || apiName.isEmpty()) {
            apiName = api.getApiPath() != null ? api.getApiPath() : "API " + (index + 1);
        }
        int listWidth = list.getWidth();
        if (listWidth <= 0) {
            listWidth = UiStyles.LIST_PANEL_WIDTH;
        }
        int maxNamePx = Math.max(40, listWidth - SIDEBAR_RESERVED_FOR_BADGE_STRUT_CHECKBOX_PX);
        FontMetrics fm = nameLabel.getFontMetrics(nameLabel.getFont());
        String shown = truncateWithEllipsis(apiName, fm, maxNamePx);
        nameLabel.setText(shown);
        nameLabel.setToolTipText(shown.equals(apiName) ? null : tooltipHtmlForPlainText(apiName));

        // 通过 ClientProperty 读取外部维护的选中索引集合
        @SuppressWarnings("unchecked")
        java.util.Set<Integer> selected = (java.util.Set<Integer>) list.getClientProperty("selectedIndices");
        boolean isChecked = selected != null && selected.contains(index);
        checkBox.setSelected(isChecked);

        if (isSelected) {
            setBackground(UiStyles.SELECTED_BG);
            nameLabel.setForeground(Color.WHITE);
        } else {
            setBackground(UiStyles.BG_CONTENT);
            nameLabel.setForeground(UiStyles.TEXT_NORMAL);
        }

        return this;
    }

    private String getMethod(ScannedApi api) {
        if (api.getRequestConfig() != null && api.getRequestConfig().getMethod() != null) {
            return api.getRequestConfig().getMethod().toUpperCase();
        }
        return "GET";
    }

    private static String truncateWithEllipsis(String text, FontMetrics fm, int maxTextPx) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (fm.stringWidth(text) <= maxTextPx) {
            return text;
        }
        final String ellipsis = "...";
        int ellipsisW = fm.stringWidth(ellipsis);
        int maxPrefix = Math.max(0, maxTextPx - ellipsisW);
        if (maxPrefix == 0) {
            return ellipsis;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (fm.stringWidth(text.substring(0, mid)) <= maxPrefix) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low > 0 ? text.substring(0, low) + ellipsis : ellipsis;
    }

    private static String tooltipHtmlForPlainText(String plain) {
        String esc = plain.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:480px'>" + esc.replace("\n", "<br/>") + "</body></html>";
    }
}
