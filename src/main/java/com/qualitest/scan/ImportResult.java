package com.qualitest.scan;

import lombok.*;

import java.util.List;

/**
 * 插件调用服务端导入接口后，从响应 JSON 解析出的结果。
 * 用于进度结束后的右下角通知文案。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** 服务端实际处理的接口条数 */
    private int totalCount;

    /** 插件本次请求发送的接口条数 */
    private int sentCount;

    /** 处理成功条数（含新增与更新） */
    private int successCount;

    /** 新增条数 */
    private int insertCount;

    /** 更新条数 */
    private int updateCount;

    /** 失败条数 */
    private int failCount;

    /** 逐条处理明细；失败时可用来展示路径与原因 */
    private List<ImportDetail> details;

    /**
     * 更新接口后的测试流影响计数。
     * 通知里只用受影响流数与告警数拼一行摘要。
     */
    private SyncImpact syncImpact;

    /** 服务端或本地生成的说明文案 */
    private String message;

    /** 业务是否成功（根据响应 code / success 判断） */
    private boolean success;

    /**
     * 单条接口的导入处理结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportDetail {
        private String apiPath;
        private String apiName;
        /** 处理状态：insert / update / fail 等 */
        private String status;
        private String message;
        private Long testProjectApiId;
    }

    /**
     * 同步影响计数：变更了多少 API、牵涉多少测试流、共有多少语义告警。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncImpact {
        /** 参与影响扫描的变更 API 个数 */
        private int changedApiCount;
        /** 可能受影响的测试流个数 */
        private int affectedFlowCount;
        /** 语义告警总条数 */
        private int warningCount;
    }
}
