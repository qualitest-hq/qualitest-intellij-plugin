package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

/**
 * 请求配置（对应 {@code request_config} 列）。
 *
 * @author qualitest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestConfig {

    public static final int CONFIG_VERSION = 1;

    @SerializedName("configVersion")
    @Builder.Default
    private int configVersion = CONFIG_VERSION;

    @SerializedName("method")
    private String method;

    @SerializedName("pathParams")
    private List<ApiParameter> pathParams;

    @SerializedName("queryParams")
    private List<ApiParameter> queryParams;

    @SerializedName("declaredHeaders")
    private List<ApiParameter> declaredHeaders;

    @SerializedName("body")
    private RequestBody body;
}
