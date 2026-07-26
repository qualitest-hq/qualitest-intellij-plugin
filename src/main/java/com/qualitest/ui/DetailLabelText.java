package com.qualitest.ui;

import java.awt.*;
/**
 * 详情区标签文案：截断、tooltip HTML、单行说明压缩。
 */
final class DetailLabelText {

    private DetailLabelText() {
    }

    static String truncateWithEllipsis(String text, FontMetrics fm, int maxTextPx) {
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

    static String tooltipHtmlForPlainText(String plain) {
        String esc = plain.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:480px'>" + esc.replace("\n", "<br/>") + "</body></html>";
    }

    /** 单行展示：压缩空白并限制长度。 */
    static String formatInlineDescription(String raw) {
        String t = raw.trim().replaceAll("\\s+", " ");
        final int max = 120;
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 1) + "\u2026";
    }
}
