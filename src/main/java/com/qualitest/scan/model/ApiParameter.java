package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * API参数信息。
 * 用于描述 API 的路径参数、查询参数、请求头等。
 *
 * @author qualitest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameter {

    @SerializedName("name")
    private String name;

    /** 参数位置：path / query / header */
    @SerializedName("in")
    private String in;

    @SerializedName("value")
    private String value;

    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("required")
    private boolean required;

    @SerializedName("schema")
    private Map<String, Object> schema;
}
