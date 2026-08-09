package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口鉴权标签，随扫描结果上传。
 * <p>
 * 告诉服务端该接口是否需要登录：
 * none=免登录；inherit=需要登录并继承项目鉴权配置；override=本接口自定义鉴权头（扫描当前不写此值）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAuthConfig {

    /** 免登录。 */
    public static final String MODE_NONE = "none";

    /** 需要登录，继承项目鉴权配置。 */
    public static final String MODE_INHERIT = "inherit";

    /** 本接口自定义鉴权头。 */
    public static final String MODE_OVERRIDE = "override";

    /**
     * 鉴权模式：none / inherit / override。
     */
    @SerializedName("mode")
    private String mode;

    /**
     * 选用的鉴权配置 id。
     * 可空：为空时由服务端按接口路径匹配；有值则直接使用指定配置。
     */
    @SerializedName("authProfileId")
    private String authProfileId;

    /** 构造免登录标签。 */
    public static ApiAuthConfig none() {
        return ApiAuthConfig.builder().mode(MODE_NONE).build();
    }

    /** 构造「需要登录、继承项目配置」标签。 */
    public static ApiAuthConfig inherit() {
        return ApiAuthConfig.builder().mode(MODE_INHERIT).build();
    }
}
