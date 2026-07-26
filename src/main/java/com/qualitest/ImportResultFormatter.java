package com.qualitest;

import com.qualitest.scan.ImportResult;

import java.util.stream.Collectors;

/**
 * 把导入结果格式化成 IDE 右下角通知里的多行文案。
 */
public final class ImportResultFormatter {

    private ImportResultFormatter() {}

    /**
     * 生成通知正文：
     * 先写发送/处理/新增/更新/成功/失败汇总；
     * 有失败时附最多 5 条失败路径与原因；
     * 有受影响测试流时再追加一行「可能受影响流: N（告警 M）」。
     */
    public static String formatSummaryMultiline(ImportResult result) {
        int sent = result.getSentCount() > 0 ? result.getSentCount() : result.getTotalCount();
        StringBuilder sb = new StringBuilder(QualiTestBundle.message(
                "message.import.summary",
                sent,
                result.getTotalCount(),
                result.getInsertCount(),
                result.getUpdateCount(),
                result.getSuccessCount(),
                result.getFailCount()
        ));
        if (result.getFailCount() > 0 && result.getDetails() != null && !result.getDetails().isEmpty()) {
            String failures = result.getDetails().stream()
                    .filter(d -> "fail".equals(d.getStatus()))
                    .limit(5)
                    .map(d -> d.getApiPath() + ": " + d.getMessage())
                    .collect(Collectors.joining("\n"));
            if (!failures.isEmpty()) {
                sb.append("\n\n").append(QualiTestBundle.message("message.import.failures.header"));
                sb.append("\n").append(failures);
                long more = result.getFailCount() - Math.min(5, result.getDetails().stream()
                        .filter(d -> "fail".equals(d.getStatus())).count());
                if (more > 0) {
                    sb.append("\n").append(QualiTestBundle.message("message.import.failures.more", more));
                }
            }
        }
        // 有受影响流时追加一行摘要，不列流明细
        ImportResult.SyncImpact impact = result.getSyncImpact();
        if (impact != null && impact.getAffectedFlowCount() > 0) {
            sb.append("\n").append(QualiTestBundle.message(
                    "message.import.impact.summary",
                    impact.getAffectedFlowCount(),
                    impact.getWarningCount()
            ));
        }
        return sb.toString();
    }
}
