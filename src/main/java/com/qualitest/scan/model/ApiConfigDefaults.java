package com.qualitest.scan.model;

/**
 * API 配置上传时的最小默认 JSON（{@code configVersion: 1}）。
 * <p>
 * 扫描结果缺少 requestConfig 或 responseConfig 时，插件用此处常量填充后再提交导入接口。
 */
public final class ApiConfigDefaults {

    /** 导入包与配置块的协议版本号 */
    public static final int CONFIG_VERSION = 1;

    /**
     * 空请求配置：GET、三组空参数数组、body.mode=none。
     */
    public static final String MIN_REQUEST_CONFIG_JSON =
            "{\"configVersion\":1,\"method\":\"GET\",\"queryParams\":[],\"pathParams\":[],"
                    + "\"declaredHeaders\":[],\"body\":{\"mode\":\"none\",\"json\":{\"schema\":null,\"example\":null}}}";

    /**
     * 空响应配置：一条 200/json、schema 与 example 均为 null 的默认成功项。
     */
    public static final String MIN_RESPONSE_CONFIG_JSON =
            "{\"configVersion\":1,\"responses\":[{\"id\":\"resp-default\",\"name\":\"成功\","
                    + "\"httpStatus\":200,\"contentType\":\"json\",\"schema\":null,\"example\":null}]}";

    private ApiConfigDefaults() {
    }
}
