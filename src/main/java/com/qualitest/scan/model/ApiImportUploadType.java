package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;

/**
 * 接口导入上传类型（与质衡 {@code ApiImportUploadType} 对齐）。
 */
public enum ApiImportUploadType {

    /** Tools → 项目级上传 */
    @SerializedName("project")
    PROJECT,

    /** Controller 上传全部 */
    @SerializedName("controllerAll")
    CONTROLLER_ALL,

    /** Controller 选择上传 */
    @SerializedName("controllerSelect")
    CONTROLLER_SELECT;

    /** 序列化到 JSON 的 code（Gson 用 {@link SerializedName}；日志用本方法）。 */
    public String code() {
        return switch (this) {
            case PROJECT -> "project";
            case CONTROLLER_ALL -> "controllerAll";
            case CONTROLLER_SELECT -> "controllerSelect";
        };
    }
}
