package com.qualitest.scan.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 请求体配置（嵌在 {@link RequestConfig#body} 下）。
 * 支持多种 body 模式：none、json、form-data、x-www-form-urlencoded、text、binary。
 *
 * @author qualitest
 */
public class RequestBody {

    /**
     * Body 模式类型，与序列化值一一对应。
     */
    public enum BodyMode {
        @SerializedName("none")   NONE("none"),
        @SerializedName("json")   JSON("json"),
        @SerializedName("form-data") FORM_DATA("form-data"),
        @SerializedName("x-www-form-urlencoded") URLENCODED("x-www-form-urlencoded"),
        @SerializedName("text")    TEXT("text"),
        @SerializedName("binary")  BINARY("binary"),
        @SerializedName("xml")     XML("xml");

        private final String value;
        BodyMode(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonBody {
        @SerializedName("schema")
        private Map<String, Object> schema;

        @SerializedName("example")
        private Object example;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormDataItem {
        @SerializedName("name")        private String name;
        @SerializedName("value")       private String value;
        @SerializedName("type")        private String type;
        @SerializedName("description")  private String description;
        @SerializedName("required")     private boolean required;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinaryBody {
        @SerializedName("type")        private String type = "file";
        @SerializedName("description") private String description;
    }

    @SerializedName("mode")         private String mode = BodyMode.NONE.getValue();
    @SerializedName("json")          private JsonBody json;
    @SerializedName("formData")     private List<FormDataItem> formData;
    @SerializedName("urlencoded")    private List<FormDataItem> urlencoded;
    @SerializedName("text")         private String text;
    @SerializedName("binary")        private BinaryBody binary;

    private RequestBody() {}

    public static RequestBody none() {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.NONE.getValue();
        return body;
    }

    public static RequestBody json(Map<String, Object> schema) {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.JSON.getValue();
        body.json = new JsonBody(schema, null);
        return body;
    }

    public static RequestBody formData(List<FormDataItem> items) {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.FORM_DATA.getValue();
        body.formData = items;
        return body;
    }

    public static RequestBody urlencoded(List<FormDataItem> items) {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.URLENCODED.getValue();
        body.urlencoded = items;
        return body;
    }

    public static RequestBody text(String content) {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.TEXT.getValue();
        body.text = content;
        return body;
    }

    public static RequestBody binary(String description) {
        RequestBody body = new RequestBody();
        body.mode = BodyMode.BINARY.getValue();
        body.binary = new BinaryBody("file", description);
        return body;
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public JsonBody getJson() { return json; }
    public void setJson(JsonBody json) { this.json = json; }
    public List<FormDataItem> getFormData() { return formData; }
    public void setFormData(List<FormDataItem> formData) { this.formData = formData; }
    public List<FormDataItem> getUrlencoded() { return urlencoded; }
    public void setUrlencoded(List<FormDataItem> urlencoded) { this.urlencoded = urlencoded; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public BinaryBody getBinary() { return binary; }
    public void setBinary(BinaryBody binary) { this.binary = binary; }
}
