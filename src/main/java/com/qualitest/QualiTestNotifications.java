package com.qualitest;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * 统一的 IDE 通知入口，避免在各 Action 中重复声明 NotificationGroup。
 */
public final class QualiTestNotifications {

    private static final NotificationGroup GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("QualiTest API Scanner");

    private QualiTestNotifications() {}

    public static void showInfo(@Nullable Project project, String title, String message) {
        Notification n = GROUP.createNotification(title, message, NotificationType.INFORMATION);
        if (project != null) {
            n.notify(project);
        }
    }

    public static void showError(@Nullable Project project, String message) {
        Notification n = GROUP.createNotification(
                QualiTestBundle.message("notification.error.title"),
                message,
                NotificationType.ERROR
        );
        if (project != null) {
            n.notify(project);
        }
    }
}
