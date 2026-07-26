package com.qualitest.ui;

import com.qualitest.QualiTestBundle;
import com.qualitest.scan.UploadScanStats;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * 上传统计区：双列卡片展示全部接口 / 已标注分组数量。
 */
public final class UploadStatsPanel extends JPanel {

    private final StatCard totalCard;
    private final StatCard explicitCard;

    public UploadStatsPanel() {
        super(new GridLayout(1, 2, 8, 0));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        totalCard = new StatCard(
                QualiTestBundle.message("upload.stats.total.label"),
                UiStyles.ACCENT_BLUE,
                UiStyles.UPLOAD_ACCENT_NUM
        );
        explicitCard = new StatCard(
                QualiTestBundle.message("upload.stats.explicit.group.label"),
                UiStyles.UPLOAD_SUCCESS_DOT,
                UiStyles.UPLOAD_SUCCESS_NUM,
                true
        );

        add(totalCard);
        add(explicitCard);
    }

    public void updateStats(UploadScanStats stats) {
        totalCard.updateMetrics(stats.getTotalApiCount(), stats.getTotalControllerCount());
        explicitCard.updateMetrics(stats.getExplicitApiCount(), stats.getExplicitControllerCount());
        explicitCard.updateDiff(
                stats.getTotalApiCount() - stats.getExplicitApiCount(),
                stats.getTotalControllerCount() - stats.getExplicitControllerCount()
        );
    }

    private static final class StatCard extends JPanel {

        private final JPanel diffContainer;
        private final JLabel apiNumLabel;
        private final JLabel ctrlNumLabel;
        private final boolean showDiff;

        StatCard(String title, Color dotColor, Color numberColor, boolean showDiff) {
            super();
            this.showDiff = showDiff;
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(UiStyles.BG_CARD);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiStyles.UPLOAD_BORDER_SOFT, 1, true),
                    new EmptyBorder(0, 0, 14, 0)
            ));

            add(createAccentLine(dotColor), BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            body.setBorder(new EmptyBorder(10, 10, 0, 10));
            body.add(createTitleRow(title, dotColor));
            body.add(Box.createVerticalStrut(8));

            apiNumLabel = createNumberLabel(numberColor);
            ctrlNumLabel = createNumberLabel(numberColor);
            body.add(createMetricRow(apiNumLabel, QualiTestBundle.message("upload.stats.metric.api")));
            body.add(Box.createVerticalStrut(6));
            body.add(createMetricRow(ctrlNumLabel, QualiTestBundle.message("upload.stats.metric.controller")));

            diffContainer = new JPanel();
            diffContainer.setLayout(new BoxLayout(diffContainer, BoxLayout.Y_AXIS));
            diffContainer.setOpaque(false);
            diffContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (showDiff) {
                body.add(Box.createVerticalStrut(10));
                body.add(createDiffSeparator());
                body.add(Box.createVerticalStrut(8));
                body.add(diffContainer);
            }

            add(body, BorderLayout.CENTER);
        }

        StatCard(String title, Color dotColor, Color numberColor) {
            this(title, dotColor, numberColor, false);
        }

        void updateMetrics(int apiCount, int controllerCount) {
            apiNumLabel.setText(String.valueOf(apiCount));
            ctrlNumLabel.setText(String.valueOf(controllerCount));
        }

        void updateDiff(int diffApi, int diffCtrl) {
            if (!showDiff) {
                return;
            }
            diffContainer.removeAll();
            diffContainer.add(UploadUiText.createDiffText(diffApi, diffCtrl));
            diffContainer.revalidate();
            diffContainer.repaint();
        }

        private static JComponent createDiffSeparator() {
            JPanel line = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UiStyles.UPLOAD_BORDER_SOFT);
                    float[] dash = {4f, 4f};
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                    g2.dispose();
                }
            };
            line.setOpaque(false);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            line.setPreferredSize(new Dimension(0, 1));
            return line;
        }

        private static JPanel createAccentLine(Color color) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(0, 8, 0, 8));
            JPanel line = new JPanel();
            line.setPreferredSize(new Dimension(0, 2));
            line.setBackground(color);
            line.setOpaque(true);
            wrap.add(line, BorderLayout.CENTER);
            return wrap;
        }

        private static JPanel createTitleRow(String title, Color dotColor) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(new JLabel(new DotIcon(dotColor)));
            JLabel label = new JLabel(title);
            label.setFont(UiStyles.DEFAULT_FONT.deriveFont(Font.BOLD, 11f));
            label.setForeground(UiStyles.UPLOAD_TEXT_DIM);
            row.add(label);
            return row;
        }

        private static JLabel createNumberLabel(Color numberColor) {
            JLabel label = new JLabel("0");
            label.setFont(UiStyles.MONO_FONT.deriveFont(Font.BOLD, 20f));
            label.setForeground(numberColor);
            return label;
        }

        private static JPanel createMetricRow(JLabel numberLabel, String unit) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(numberLabel);
            JLabel unitLabel = new JLabel(unit);
            unitLabel.setFont(UiStyles.DEFAULT_FONT.deriveFont(12f));
            unitLabel.setForeground(UiStyles.TEXT_SECONDARY);
            row.add(unitLabel);
            return row;
        }
    }

    private static final class DotIcon implements Icon {

        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 2, 6, 6);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 6;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}
