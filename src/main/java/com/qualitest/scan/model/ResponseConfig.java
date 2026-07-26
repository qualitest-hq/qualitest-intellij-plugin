package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 响应配置（对应 {@code response_config} 列）。
 *
 * @author qualitest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseConfig {

    public static final int CONFIG_VERSION = 1;

    @SerializedName("configVersion")
    @Builder.Default
    private int configVersion = CONFIG_VERSION;

    @SerializedName("responses")
    private List<ResponseItem> responses;

    /**
     * 单条响应定义（{@code responses[]} 元素）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseItem {

        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("httpStatus")
        private int httpStatus;

        @SerializedName("contentType")
        private String contentType;

        @SerializedName("schema")
        private java.util.Map<String, Object> schema;

        @SerializedName("example")
        private Object example;
    }

    /**
     * 稳定响应 id：hash(method + apiPath + httpStatus + contentType)，不含 name。
     */
    public static String buildStableResponseId(String method, String apiPath, int httpStatus, String contentType) {
        String seed = (method != null ? method : "")
                + (apiPath != null ? apiPath : "")
                + httpStatus
                + (contentType != null ? contentType : "");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("resp-");
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "resp-" + Integer.toHexString(seed.hashCode());
        }
    }
}
