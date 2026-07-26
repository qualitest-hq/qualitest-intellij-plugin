package com.qualitest.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.qualitest.scan.ApiScannerSupport;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.visitor.ControllerVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Controller 右键上传相关：从编辑器上下文解析当前 Java 文件中的 Controller，并扫描其 API。
 */
public final class ControllerUploadSupport {

    private ControllerUploadSupport() {}

    public enum Kind {
        NO_JAVA_FILE,
        NO_CONTROLLER,
        OK
    }

    public record Result(@NotNull Kind kind, @Nullable PsiClass controller) {}

    @NotNull
    public static Result resolve(@NotNull Project project, @NotNull AnActionEvent e) {
        PsiFile psiFile = getCurrentPsiFile(project, e);
        if (psiFile == null || !(psiFile instanceof PsiJavaFile javaFile)) {
            return new Result(Kind.NO_JAVA_FILE, null);
        }
        PsiClass controller = findFirstController(javaFile);
        if (controller == null) {
            return new Result(Kind.NO_CONTROLLER, null);
        }
        return new Result(Kind.OK, controller);
    }

    @NotNull
    public static List<ScannedApi> scanController(@NotNull Project project, @NotNull PsiClass controller) {
        return ApiScannerSupport.forProject(project).scanController(controller);
    }

    @Nullable
    private static PsiFile getCurrentPsiFile(@NotNull Project project, @NotNull AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null) {
            VirtualFile virtualFile = editor.getVirtualFile();
            if (virtualFile != null) {
                return PsiManager.getInstance(project).findFile(virtualFile);
            }
        }
        return e.getData(CommonDataKeys.PSI_FILE);
    }

    @Nullable
    private static PsiClass findFirstController(@NotNull PsiJavaFile javaFile) {
        ControllerVisitor visitor = new ControllerVisitor();
        List<PsiClass> controllers = visitor.findControllers(javaFile);
        return controllers.isEmpty() ? null : controllers.get(0);
    }
}
