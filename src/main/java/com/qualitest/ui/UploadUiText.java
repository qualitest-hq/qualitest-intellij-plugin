package com.qualitest.ui;

import com.intellij.ui.components.JBLabel;
import com.qualitest.QualiTestBundle;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;

/**
 * 上传确认相关 UI：properties 保留完整句子，样式在 Java 中套用。
 */
public final class UploadUiText {

    private UploadUiText() {}

    public static JComponent createFilterHint() {
        JBLabel label = new JBLabel(buildFilterHintHtml(QualiTestBundle.message("upload.filter.hint")));
        label.setAllowAutoWrapping(true);
        label.setCopyable(false);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JComponent createDiffText(int diffApi, int diffCtrl) {
        if (diffApi == 0 && diffCtrl == 0) {
            JLabel equal = new JLabel(QualiTestBundle.message("upload.stats.diff.equal"));
            equal.setFont(UiStyles.DEFAULT_FONT.deriveFont(11f));
            equal.setForeground(UiStyles.UPLOAD_TEXT_DIM);
            equal.setAlignmentX(Component.LEFT_ALIGNMENT);
            return equal;
        }
        String text = MessageFormat.format(
                QualiTestBundle.message("upload.stats.diff"), diffApi, diffCtrl);
        JBLabel label = new JBLabel(buildDiffHtml(text));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String buildFilterHintHtml(String text) {
        String html = escapeHtml(text);
        html = highlightCodeToken(html, "@api.group");
        html = highlightCodeToken(html, "@Tag");
        return "<html><body style='width:" + UiStyles.UPLOAD_CONFIRM_HINT_WIDTH + "px;color:"
                + UiStyles.toHtmlHex(UiStyles.UPLOAD_TEXT_DIM)
                + ";font-size:12px;line-height:1.55'>" + html + "</body></html>";
    }

    private static String buildDiffHtml(String text) {
        String html = escapeHtml(text).replaceAll(
                "(\\d+)",
                "<b style='color:" + UiStyles.toHtmlHex(UiStyles.UPLOAD_TEXT_MUTED) + ";'>$1</b>"
        );
        return "<html><span style='color:" + UiStyles.toHtmlHex(UiStyles.UPLOAD_TEXT_DIM)
                + ";font-size:11px'>" + html + "</span></html>";
    }

    private static String highlightCodeToken(String html, String token) {
        return html.replace(
                token,
                "<font face='monospaced' color='" + UiStyles.toHtmlHex(UiStyles.UPLOAD_CODE_TAG) + "'>"
                        + token + "</font>"
        );
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
