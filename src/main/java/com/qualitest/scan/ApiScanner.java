package com.qualitest.scan;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestConstants;
import com.qualitest.scan.extractor.ApiInfoExtractor;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.visitor.ControllerVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * API 扫描器：基于 IntelliJ PSI 从 Controller 提取 API 元信息。
 *
 * <p>扫描流水线：Java 文件索引 → 识别 Controller → 遍历 API 方法 → {@link ApiInfoExtractor} 提取。
 *
 * <p>线程与取消约定：
 * <ul>
 *   <li>所有 PSI 访问通过 {@link #runInReadAction} 在 {@link ReadAction#nonBlocking} 中执行</li>
 *   <li>{@code inSmartMode} 会等待索引就绪，避免 {@link com.intellij.openapi.project.IndexNotReadyException}</li>
 *   <li>传入 {@link ProgressIndicator} 时绑定 {@code wrapProgress}，循环内调用 {@link ProgressManager#checkCanceled()}</li>
 *   <li>用户取消时抛出 {@link com.intellij.openapi.progress.ProcessCanceledException}</li>
 * </ul>
 */
public class ApiScanner {

    private static final Logger LOG = Logger.getInstance(ApiScanner.class);

    private final Project project;
    private final ApiInfoExtractor extractor;
    private final ControllerVisitor visitor;
    private final ExplicitGroupChecker explicitGroupChecker;

    public ApiScanner(Project project, String groupTag, boolean ignoreFirstGroupLevel) {
        this(project, groupTag, ignoreFirstGroupLevel, true, QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS);
    }

    /**
     * @param excludeJsonIgnoreFields 为 true 时，上传文档不包含带 JsonIgnore 的模型字段
     */
    public ApiScanner(Project project, String groupTag, boolean ignoreFirstGroupLevel, boolean excludeJsonIgnoreFields) {
        this(project, groupTag, ignoreFirstGroupLevel, excludeJsonIgnoreFields, QualiTestConstants.DEFAULT_ANONYMOUS_ANNOTATIONS);
    }

    /**
     * @param excludeJsonIgnoreFields 为 true 时，上传文档不包含带 JsonIgnore 的模型字段
     * @param anonymousAnnotations    免登录注解名单，用于给扫描结果打鉴权标签
     */
    public ApiScanner(
            Project project,
            String groupTag,
            boolean ignoreFirstGroupLevel,
            boolean excludeJsonIgnoreFields,
            java.util.Collection<String> anonymousAnnotations) {
        this.project = project;
        this.extractor = new ApiInfoExtractor(groupTag, ignoreFirstGroupLevel, excludeJsonIgnoreFields, anonymousAnnotations);
        this.visitor = new ControllerVisitor();
        this.explicitGroupChecker = new ExplicitGroupChecker(groupTag);
    }

    // -------------------------------------------------------------------------
    // 公开 API
    // -------------------------------------------------------------------------

    /**
     * 扫描整个项目中的 API（无进度、不可取消）。
     */
    public List<ScannedApi> scanProject() {
        return scanProject(ProjectScope.getProjectScope(project), null);
    }

    /**
     * 扫描整个项目；{@code indicator} 非空时展示进度并支持用户取消。
     */
    public List<ScannedApi> scanProject(@Nullable ProgressIndicator indicator) {
        return scanProject(ProjectScope.getProjectScope(project), indicator);
    }

    /**
     * 在指定搜索范围内扫描 API（无进度、不可取消）。
     */
    public List<ScannedApi> scanProject(@NotNull GlobalSearchScope scope) {
        return scanProject(scope, null);
    }

    /**
     * 在指定搜索范围内扫描 API；{@code indicator} 非空时展示进度并支持用户取消。
     */
    public List<ScannedApi> scanProject(@NotNull GlobalSearchScope scope, @Nullable ProgressIndicator indicator) {
        return runInReadAction(() -> collectApisFromScope(scope, indicator), indicator);
    }

    /**
     * 扫描单个 Controller 中的 API（无进度、不可取消）。
     */
    public List<ScannedApi> scanController(@NotNull PsiClass controller) {
        return scanController(controller, null);
    }

    /**
     * 扫描单个 Controller；{@code indicator} 非空时支持取消（通常仅项目级扫描会传入）。
     */
    public List<ScannedApi> scanController(@NotNull PsiClass controller, @Nullable ProgressIndicator indicator) {
        return runInReadAction(() -> collectApisFromController(controller), indicator);
    }

    // -------------------------------------------------------------------------
    // 项目级扫描
    // -------------------------------------------------------------------------

    /**
     * 遍历 scope 内全部 .java 文件，汇总其中 Controller 的 API。
     * 必须在读锁内调用。
     */
    private List<ScannedApi> collectApisFromScope(GlobalSearchScope scope, @Nullable ProgressIndicator indicator) {
        Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(
                FileTypeManager.getInstance().getFileTypeByExtension("java"),
                scope
        );

        List<ScannedApi> apis = new ArrayList<>();
        int total = javaFiles.size();
        ScanProgress progress = ScanProgress.start(indicator, total);

        int index = 0;
        for (VirtualFile file : javaFiles) {
            ProgressManager.checkCanceled();
            progress.tick(index++, file);

            try {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile instanceof PsiJavaFile javaFile) {
                    apis.addAll(collectApisFromFile(javaFile));
                }
            } catch (Exception e) {
                // 依赖缺失等导致 PSI 解析失败时跳过该文件
            }
        }
        progress.finish();
        return apis;
    }

    /**
     * 从单个 Java 源文件中收集所有 Controller 的 API。必须在读锁内调用。
     */
    private List<ScannedApi> collectApisFromFile(@NotNull PsiJavaFile file) {
        List<ScannedApi> apis = new ArrayList<>();
        for (PsiClass controller : visitor.findControllers(file)) {
            ProgressManager.checkCanceled();
            apis.addAll(collectApisFromController(controller));
        }
        return apis;
    }

    // -------------------------------------------------------------------------
    // Controller 级扫描
    // -------------------------------------------------------------------------

    /**
     * 提取单个 Controller 的全部 API 方法。非 Controller 返回空列表。必须在读锁内调用。
     */
    private List<ScannedApi> collectApisFromController(@NotNull PsiClass controller) {
        if (!visitor.isController(controller)) {
            return List.of();
        }

        String qualifiedName = controller.getQualifiedName();
        boolean hasExplicitGroup = explicitGroupChecker.hasExplicitGroup(controller);

        List<ScannedApi> apis = new ArrayList<>();
        for (PsiMethod method : visitor.getApiMethods(controller)) {
            ProgressManager.checkCanceled();
            try {
                ScannedApi api = extractor.extract(controller, method);
                api.setControllerQualifiedName(qualifiedName);
                api.setControllerHasExplicitGroup(hasExplicitGroup);
                apis.add(api);
            } catch (Exception e) {
                LOG.warn("Failed to extract API: " + method.getName(), e);
            }
        }
        return apis;
    }

    // -------------------------------------------------------------------------
    // 读操作与进度
    // -------------------------------------------------------------------------

    /**
     * 在可取消的非阻塞读操作中执行 {@code computation}。
     * {@code indicator} 非空时与读操作进度同步，用户点取消会中断计算。
     */
    private <T> T runInReadAction(@NotNull Callable<T> computation, @Nullable ProgressIndicator indicator) {
        var builder = ReadAction.nonBlocking(computation).inSmartMode(project);
        if (indicator != null) {
            builder = builder.wrapProgress(indicator);
        }
        return builder.executeSynchronously();
    }

    /**
     * 项目级扫描进度：按已处理文件数更新进度条，副标题显示当前文件路径。
     */
    private static final class ScanProgress {
        private final @Nullable ProgressIndicator indicator;
        private final int total;

        private ScanProgress(@Nullable ProgressIndicator indicator, int total) {
            this.indicator = indicator;
            this.total = total;
        }

        static ScanProgress start(@Nullable ProgressIndicator indicator, int total) {
            ScanProgress progress = new ScanProgress(indicator, total);
            progress.tick(0, null);
            return progress;
        }

        void tick(int processed, @Nullable VirtualFile currentFile) {
            if (indicator == null) {
                return;
            }
            indicator.setText(QualiTestBundle.message("progress.scanning.controllers"));
            if (total <= 0) {
                indicator.setIndeterminate(true);
                indicator.setText2(null);
                return;
            }
            indicator.setIndeterminate(false);
            indicator.setFraction(Math.min(1.0, (double) processed / total));
            if (currentFile != null) {
                indicator.setText2(currentFile.getPresentableUrl());
            }
        }

        void finish() {
            if (indicator != null && total > 0) {
                indicator.setFraction(1.0);
            }
        }
    }
}
