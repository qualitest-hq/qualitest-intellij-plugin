package com.qualitest.ui.render;

import java.awt.*;

/**
 * UI 样式常量。
 * 颜色遵循 IntelliJ Darcula / VS Code 暗色主题的语义配色规范。
 * HTTP 方法使用语义色（GET=青、POST=蓝、PUT=黄、DELETE=红、PATCH=紫），
 * 参数类型使用绿色，强调色使用蓝色。
 *
 * @author qualitest
 */
public final class UiStyles {

    private UiStyles() {} // 禁止实例化

    // ---- 背景色 ----
    public static final Color BG_PANEL   = new Color(0x252526);
    public static final Color BG_HEADER   = new Color(0x2d2d30);
    public static final Color BG_CONTENT = new Color(0x1e1e1e);
    public static final Color BG_CARD    = new Color(0x2d2d30);

    // ---- 文本色 ----
    public static final Color BORDER_COLOR   = new Color(0x3c3c3c);
    public static final Color TEXT_PRIMARY   = new Color(0xcccccc);
    public static final Color TEXT_SECONDARY = new Color(0x808080);
    public static final Color TEXT_NORMAL    = new Color(0xd4d4d4);

    // ---- HTTP 方法语义色 ----
    public static final Color METHOD_GET    = new Color(0x4ec9b0);
    public static final Color METHOD_POST   = new Color(0x569cd6);
    public static final Color METHOD_PUT    = new Color(0xdcdcaa);
    public static final Color METHOD_DELETE = new Color(0xf14c4c);
    public static final Color METHOD_PATCH  = new Color(0xc586c0);

    // ---- 强调色 ----
    public static final Color ACCENT_BLUE       = new Color(0x569cd6);
    public static final Color ACCENT_LIGHT_BLUE = new Color(0x4fc1ff);
    public static final Color SELECTED_BG      = new Color(0x094771);
    public static final Color PARAM_GREEN      = new Color(0x6a9955);
    public static final Color REQUIRED_BG      = new Color(0x3c1f1f);
    public static final Color OPTIONAL_BG      = new Color(0x1f3c1f);

    // ---- 上传确认框（对齐 preview/upload-confirm-dialog.html）----
    public static final Color UPLOAD_BORDER_SOFT = new Color(0x3d4046);
    public static final Color UPLOAD_ACCENT_NUM  = new Color(0x6eb1ff);
    public static final Color UPLOAD_SUCCESS_NUM = new Color(0x7ddea8);
    public static final Color UPLOAD_SUCCESS_DOT = new Color(0x3dd68c);
    public static final Color UPLOAD_FILTER_BG   = new Color(0x1e1f22);
    public static final Color UPLOAD_TEXT_DIM    = new Color(0x6e7681);
    public static final Color UPLOAD_TEXT_MUTED  = new Color(0x9aa0a6);
    public static final Color UPLOAD_CODE_TAG    = new Color(0xa9b7c6);

    /** 项目级上传确认框内容区宽度（像素，不含 IDE 对话框边距） */
    public static final int UPLOAD_CONFIRM_WIDTH = 420;
    /** 确认框内多行说明文字换行宽度 */
    public static final int UPLOAD_CONFIRM_HINT_WIDTH = 360;

    // ---- 尺寸 ----
    public static final int LIST_PANEL_WIDTH = 320;
    public static final Dimension DEFAULT_PANEL_SIZE = new Dimension(1100, 700);

    // ---- 字体 ----
    public static final Font DEFAULT_FONT = initDefaultFont();
    public static final Font MONO_FONT = initMonoFont();

    private static Font initDefaultFont() {
        String[] names = {"Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", "SimHei", "SimSun", "Segoe UI"};
        for (String name : names) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (f.canDisplay('\u4e2d')) return f;
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    private static Font initMonoFont() {
        String[] names = {"Consolas", "Source Code Pro", "JetBrains Mono", "Menlo", "Monaco"};
        for (String name : names) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (f.canDisplay('\u4e2d')) return f;
        }
        return DEFAULT_FONT;
    }

    /** 将 {@link Color} 转为 HTML 内联样式用的 #RRGGBB 字符串 */
    public static String toHtmlHex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    /** 根据 HTTP 方法返回语义背景色 */
    public static Color getMethodColor(String method) {
        return switch (method) {
            case "GET"    -> METHOD_GET;
            case "POST"   -> METHOD_POST;
            case "PUT"    -> METHOD_PUT;
            case "DELETE" -> METHOD_DELETE;
            case "PATCH"  -> METHOD_PATCH;
            default       -> METHOD_GET;
        };
    }

    /** GET/PUT 背景较浅，使用深色文字 */
    public static Color getMethodTextColor(String method) {
        return switch (method) {
            case "GET", "PUT" -> new Color(0x1e1e1e);
            default           -> Color.WHITE;
        };
    }
}
