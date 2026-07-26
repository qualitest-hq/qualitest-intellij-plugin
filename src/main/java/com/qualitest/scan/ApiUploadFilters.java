package com.qualitest.scan;

import com.qualitest.scan.model.ScannedApi;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 上传前 API 列表过滤与统计。
 */
public final class ApiUploadFilters {

    private ApiUploadFilters() {}

    @NotNull
    public static List<ScannedApi> filterByExplicitGroup(@NotNull List<ScannedApi> apis, boolean onlyExplicit) {
        if (!onlyExplicit) {
            return List.copyOf(apis);
        }
        return apis.stream()
                .filter(ScannedApi::isControllerHasExplicitGroup)
                .collect(Collectors.toList());
    }

    @NotNull
    public static UploadScanStats summarize(@NotNull List<ScannedApi> apis) {
        Set<String> allControllers = new HashSet<>();
        Set<String> explicitControllers = new HashSet<>();
        int explicitApiCount = 0;

        for (ScannedApi api : apis) {
            if (api.isControllerHasExplicitGroup()) {
                explicitApiCount++;
            }
            String controllerName = api.getControllerQualifiedName();
            if (controllerName != null && !controllerName.isBlank()) {
                allControllers.add(controllerName);
                if (api.isControllerHasExplicitGroup()) {
                    explicitControllers.add(controllerName);
                }
            }
        }

        return UploadScanStats.builder()
                .totalApiCount(apis.size())
                .totalControllerCount(allControllers.isEmpty() ? fallbackControllerCount(apis) : allControllers.size())
                .explicitApiCount(explicitApiCount)
                .explicitControllerCount(explicitControllers.size())
                .build();
    }

    private static int fallbackControllerCount(List<ScannedApi> apis) {
        if (apis.isEmpty()) {
            return 0;
        }
        return 1;
    }
}
