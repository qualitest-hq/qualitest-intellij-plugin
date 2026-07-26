package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

/**
 * IDEA 插件批量导入 API 的请求体根对象。
 * <p>
 * 包含协议版本、目标项目 id 与 {@link ApiImportItem} 列表，序列化后 POST 到服务端导入接口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiImportParams {

    /** 上传包协议版本，固定为 1 */
    public static final int CONFIG_VERSION = ApiConfigV2Defaults.CONFIG_VERSION;

    @SerializedName("configVersion")
    @Builder.Default
    private int configVersion = CONFIG_VERSION;

    @SerializedName("testProjectId")
    private Long testProjectId;

    @SerializedName("apiList")
    private List<ApiImportItem> apiList;
}
