package com.qualitest.config;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestStrings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 上传前校验服务器地址与项目令牌，未配置时引导用户打开设置页。
 */
public final class UploadConfigSupport {

    public enum Missing {
        SERVER_URL,
        PROJECT_TOKEN
    }

    private UploadConfigSupport() {}

    @Nullable
    public static Missing findMissing(@NotNull QualiTestSettings settings) {
        if (QualiTestStrings.trimToNull(settings.getServerUrl()) == null) {
            return Missing.SERVER_URL;
        }
        if (QualiTestStrings.trimToNull(settings.getProjectToken()) == null) {
            return Missing.PROJECT_TOKEN;
        }
        return null;
    }

    /**
     * @return true 表示配置完整，可继续上传；false 表示已提示用户去配置
     */
    public static boolean ensureConfigured(@Nullable Project project, @NotNull QualiTestSettings settings) {
        Missing missing = findMissing(settings);
        if (missing == null) {
            return true;
        }
        promptConfigure(project, missing);
        return false;
    }

    private static void promptConfigure(@Nullable Project project, @NotNull Missing missing) {
        String messageKey = missing == Missing.SERVER_URL
                ? "configure.server.required"
                : "configure.token.required";
        int result = Messages.showYesNoDialog(
                project,
                QualiTestBundle.message(messageKey) + "\n" + QualiTestBundle.message("configure.prompt.suffix"),
                QualiTestBundle.message("prompt.title"),
                QualiTestBundle.message("configure.go"),
                QualiTestBundle.message("configure.cancel"),
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            openSettings(project);
        }
    }

    /**
     * 打开 Qualitest Helper 设置页（Settings → Tools → Qualitest Helper）。
     */
    public static void openSettings(@Nullable Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, QualiTestConfigurable.class);
    }
}
