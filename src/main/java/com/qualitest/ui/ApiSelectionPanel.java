package com.qualitest.ui;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestNotifications;
import com.qualitest.config.QualiTestSettings;
import com.qualitest.config.UploadConfigSupport;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.render.UiStyles;
import com.qualitest.ui.render.ApiListCellRenderer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API 选择面板：左侧多选列表，右侧展示选中项的路径、参数与响应结构。
 * 列表渲染见 {@link ApiListCellRenderer}，详情卡片见 {@link ApiSelectionDetailBuilder}。
 */
public class ApiSelectionPanel extends JPanel {

    private final ApiSelectionDetailBuilder detailBuilder = new ApiSelectionDetailBuilder();

    // =====================================================================
    // 数据与状态
    // =====================================================================

    private final List<ScannedApi> apis;
    private final Set<Integer> selectedIndices = new HashSet<>();

    // =====================================================================
    // 组件引用
    // =====================================================================

    private JList<ScannedApi> apiList;
    private ApiListModel listModel;
    private JPanel detailPanel;
    private JCheckBox selectAllCheckbox;
    private JLabel selectionCountLabel;

    // =====================================================================
    // 构造
    // =====================================================================

    /**
     * 创建 API 选择面板。
     *
     * @param apis 待展示的 API 列表（Panel 内部维护一份副本）
     */
    public ApiSelectionPanel(List<ScannedApi> apis) {
        this.apis = new ArrayList<>(apis);
        setLayout(new BorderLayout(0, 0));
        setBackground(UiStyles.BG_PANEL);
        setPreferredSize(UiStyles.DEFAULT_PANEL_SIZE);
        initComponents();
    }

    // =====================================================================
    // 布局初始化
    // =====================================================================

    /**
     * 初始化全部子组件并组装布局。
     * 布局结构：CenterPane = [左侧列表 WEST] [分隔线 CENTER] [右侧详情 EAST]。
     */
    private void initComponents() {
        JPanel centerPane = new JPanel(new BorderLayout(0, 0));
        centerPane.setBackground(UiStyles.BG_PANEL);

        centerPane.add(createLeftPanel(), BorderLayout.WEST);

        // 垂直分隔线
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 0));
        separator.setBackground(UiStyles.BORDER_COLOR);
        centerPane.add(separator, BorderLayout.CENTER);

        centerPane.add(createRightPanel(), BorderLayout.CENTER);

        add(centerPane, BorderLayout.CENTER);
    }

    /**
     * 构建左侧面板：API 列表（带滚动条）+ 底部选择控制栏。
     *
     * @return 左侧面板
     */
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setPreferredSize(new Dimension(UiStyles.LIST_PANEL_WIDTH, 0));
        panel.setBackground(UiStyles.BG_CONTENT);

        // API 列表
        listModel = new ApiListModel();
        apiList = new JBList<>(listModel);
        apiList.setBackground(UiStyles.BG_CONTENT);
        apiList.setForeground(UiStyles.TEXT_NORMAL);
        apiList.setSelectionBackground(UiStyles.SELECTED_BG);
        apiList.setSelectionForeground(Color.WHITE);
        apiList.setBorder(null);
        apiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 将选中索引集合挂到 ClientProperty，Renderer 通过它读取状态（避免循环引用）
        apiList.putClientProperty("selectedIndices", selectedIndices);
        apiList.setCellRenderer(new ApiListCellRenderer());

        JScrollPane scrollPane = new JBScrollPane(apiList);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UiStyles.BG_CONTENT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        customizeScrollBar(scrollPane.getVerticalScrollBar());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // 单击列表项 → 更新右侧详情面板
        apiList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = apiList.getSelectedIndex();
                if (idx >= 0 && idx < apis.size()) {
                    updateDetailPanel(apis.get(idx));
                }
            }
        });

        // 单击复选框 → 切换该 API 的选中状态
        apiList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) {
                    return;
                }
                int idx = apiList.locationToIndex(e.getPoint());
                if (idx >= 0 && isCheckboxClick(idx, e.getPoint())) {
                    toggleSelection(idx);
                    e.consume();
                }
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createSelectionControlPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 构建底部选择控制栏：全选复选框 + 选中计数标签。
     *
     * @return 底部控制栏
     */
    private JPanel createSelectionControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        panel.setBackground(UiStyles.BG_HEADER);

        selectAllCheckbox = new JCheckBox(QualiTestBundle.message("button.select.all"));
        selectAllCheckbox.setFont(UiStyles.DEFAULT_FONT);
        selectAllCheckbox.setForeground(UiStyles.TEXT_PRIMARY);
        selectAllCheckbox.setBackground(UiStyles.BG_HEADER);
        selectAllCheckbox.setFocusPainted(false);
        selectAllCheckbox.setBorderPainted(false);
        selectAllCheckbox.setOpaque(false);
        selectAllCheckbox.setSelected(false);
        selectAllCheckbox.addActionListener(e -> selectAll(selectAllCheckbox.isSelected()));

        panel.add(selectAllCheckbox, BorderLayout.WEST);

        selectionCountLabel = new JLabel(
                QualiTestBundle.message("label.selected.count", selectedIndices.size(), apis.size())
        );
        selectionCountLabel.setFont(UiStyles.DEFAULT_FONT);
        selectionCountLabel.setForeground(UiStyles.TEXT_SECONDARY);
        panel.add(selectionCountLabel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 构建右侧详情面板：API 基本信息 + 请求参数 + 响应结构。
     * 初始显示第一个 API。
     *
     * @return 右侧详情面板
     */
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(UiStyles.BG_PANEL);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        detailPanel = new ScrollableDetailPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBackground(UiStyles.BG_PANEL);
        detailPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JBScrollPane(detailPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UiStyles.BG_PANEL);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        customizeScrollBar(scrollPane.getVerticalScrollBar());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);

        if (!apis.isEmpty()) {
            updateDetailPanel(apis.get(0));
            apiList.setSelectedIndex(0);
        } else {
            updateDetailPanel(null);
        }

        return panel;
    }

    // =====================================================================
    // 详情面板更新
    // =====================================================================

    /**
     * 刷新右侧详情面板，显示给定 API 的完整信息。
     *
     * @param api 选中的 API，null 表示清空面板
     */
    private void updateDetailPanel(ScannedApi api) {
        detailPanel.removeAll();
        if (api == null) {
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }

        addDetailCard(detailBuilder.createInfoCard(api));
        addDetailCard(detailBuilder.createParamsCard(api));
        addDetailCard(detailBuilder.createResponseCard(api));

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void addDetailCard(JPanel card) {
        if (card == null || card.getComponentCount() == 0) {
            return;
        }
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (detailPanel.getComponentCount() > 0) {
            detailPanel.add(Box.createVerticalStrut(10));
        }
        detailPanel.add(card);
    }

    /**
     * 详情区根面板：宽度跟随滚动视口，避免垂直滚动条出现时挤压内部固定宽度网格。
     */
    private static final class ScrollableDetailPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }


    // =====================================================================
    // 交互逻辑
    // =====================================================================

    private boolean isUpdatingSelection = false;

    /**
     * 全选或取消全选。
     *
     * @param select true=全选，false=取消全选
     */
    private void selectAll(boolean select) {
        isUpdatingSelection = true;
        selectedIndices.clear();

        if (select) {
            for (int i = 0; i < apis.size(); i++) {
                selectedIndices.add(i);
            }
        }

        if (selectAllCheckbox != null) {
            selectAllCheckbox.setSelected(select);
        }
        isUpdatingSelection = false;

        listModel.fireContentsChanged();
        updateSelectionCount();
    }

    /**
     * 切换指定索引的选中状态。
     *
     * @param index API 在列表中的索引
     */
    private void toggleSelection(int index) {
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index);
        } else {
            selectedIndices.add(index);
        }

        if (!isUpdatingSelection && selectAllCheckbox != null) {
            selectAllCheckbox.setSelected(selectedIndices.size() == apis.size());
        }
        listModel.fireContentsChanged();
        updateSelectionCount();
    }

    /**
     * 判断点击点是否落在列表项右侧复选框区域内。
     */
    private boolean isCheckboxClick(int index, Point point) {
        Rectangle cellBounds = apiList.getCellBounds(index, index);
        if (cellBounds == null) {
            return false;
        }
        if (!cellBounds.contains(point)) {
            return false;
        }

        int checkboxSize = 16;
        Icon checkboxIcon = UIManager.getIcon("CheckBox.icon");
        if (checkboxIcon != null) {
            checkboxSize = Math.max(checkboxIcon.getIconWidth(), checkboxIcon.getIconHeight());
        }

        // 为了避免不同 LAF / 缩放下图标像素偏差导致“偶尔点不中”，
        // 以列表项右侧区域作为复选框热区，适当放宽点击范围。
        int xInCell = point.x - cellBounds.x;
        int hotZoneWidth = Math.max(32, checkboxSize + 20);
        int hotZoneStart = cellBounds.width - hotZoneWidth;
        return xInCell >= hotZoneStart;
    }

    private void updateSelectionCount() {
        selectionCountLabel.setText(
                QualiTestBundle.message("label.selected.count", selectedIndices.size(), apis.size())
        );
    }

    // =====================================================================
    // ListModel
    // =====================================================================

    private class ApiListModel extends AbstractListModel<ScannedApi> {
        @Override public int getSize() { return apis.size(); }
        @Override public ScannedApi getElementAt(int index) { return apis.get(index); }
        public void fireContentsChanged() {
            if (apis.isEmpty()) {
                fireIntervalRemoved(this, 0, 0);
            } else {
                fireContentsChanged(this, 0, apis.size() - 1);
            }
        }
    }

    // =====================================================================
    // 工具方法
    // =====================================================================

    private void customizeScrollBar(JScrollBar scrollBar) {
        scrollBar.setPreferredSize(new Dimension(6, 0));
        scrollBar.setBackground(UiStyles.BG_CONTENT);
    }

    // =====================================================================
    // 公开 API（供 Dialog / Action 使用）
    // =====================================================================

    /**
     * 获取当前已选中的 API 列表。
     *
     * @return 选中的 ScannedApi 副本
     */
    public List<ScannedApi> getSelectedApis() {
        List<ScannedApi> result = new ArrayList<>();
        for (int i = 0; i < apis.size(); i++) {
            if (selectedIndices.contains(i)) {
                result.add(apis.get(i));
            }
        }
        return result;
    }

    /**
     * 执行上传。将选中的 API 上传至 QualiTest 服务器。
     *
     * @param project  当前项目
     * @param settings 插件配置（含服务器地址和 Project Token）
     */
    public void performUpload(Project project, QualiTestSettings settings) {
        List<ScannedApi> toUpload = getSelectedApis();
        if (toUpload.isEmpty()) {
            QualiTestNotifications.showInfo(project,
                    QualiTestBundle.message("prompt.title"),
                    QualiTestBundle.message("message.no.api.selected"));
            return;
        }
        if (!UploadConfigSupport.ensureConfigured(project, settings)) {
            return;
        }
        UploadTaskSupport.runUploadTask(
                project,
                settings.getServerUrl().trim(),
                settings.getProjectToken().trim(),
                toUpload
        );
    }
}
