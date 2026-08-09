package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.Date;

/**
 * 扫描到的 API 元信息模型。
 * 包含 API 的路径、名称、分组、请求配置、响应配置等完整信息，
 * 用于序列化后上传至 QualiTest 服务器。
 *
 * @author qualitest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScannedApi {

    @SerializedName("apiStatus")
    @Builder.Default
    private String apiStatus = "normal";

    @SerializedName("apiGroup")
    private String apiGroup;

    @SerializedName("apiName")
    private String apiName;

    @SerializedName("apiDescription")
    private String apiDescription;

    @SerializedName("apiPath")
    private String apiPath;

    @SerializedName("protocolType")
    @Builder.Default
    private String protocolType = "http";

    @SerializedName("requestConfig")
    private RequestConfig requestConfig;

    @SerializedName("responseConfig")
    private ResponseConfig responseConfig;

    @SerializedName("lastSyncTime")
    private Date lastSyncTime;

    @SerializedName("sourceSystem")
    @Builder.Default
    private String sourceSystem = "idea-plugin";

    @SerializedName("externalId")
    private String externalId;

    /**
     * 扫描时写入的鉴权标签：命中免登录注解为 none，否则为 inherit；随接口一并上传。
     */
    @SerializedName("auth")
    private ApiAuthConfig auth;

    /** 所属 Controller 全限定名，仅插件内部用于统计与过滤，不上传服务端。 */
    private String controllerQualifiedName;

    /** 所属 Controller 是否带显式分组注释，仅插件内部使用。 */
    private boolean controllerHasExplicitGroup;
}
