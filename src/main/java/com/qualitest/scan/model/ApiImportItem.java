package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.Date;

/**
 * 单个 API 导入项，序列化后作为上传包中的一条接口数据。
 *
 * @author qualitest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiImportItem {

    @SerializedName("apiStatus")
    private String apiStatus;

    @SerializedName("apiGroup")
    private String apiGroup;

    @SerializedName("apiName")
    private String apiName;

    @SerializedName("apiDescription")
    private String apiDescription;

    @SerializedName("apiPath")
    private String apiPath;

    @SerializedName("protocolType")
    private String protocolType;

    @SerializedName("requestConfig")
    private String requestConfig;

    @SerializedName("responseConfig")
    private String responseConfig;

    @SerializedName("sourceSystem")
    private String sourceSystem;

    @SerializedName("externalId")
    private String externalId;

    @SerializedName("lastSyncTime")
    private Date lastSyncTime;

    /**
     * 接口鉴权标签（是否免登录等），有则一并上传给服务端落库。
     */
    @SerializedName("auth")
    private ApiAuthConfig auth;
}
