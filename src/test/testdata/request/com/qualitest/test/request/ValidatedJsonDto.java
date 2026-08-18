package com.qualitest.test.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * 带校验注解的 JSON 请求体夹具。
 * name 标 NotNull、tags 标 NotEmpty，应进入 schema.required；note 无注解，不应进入。
 */
public class ValidatedJsonDto {

    /** 必填字符串 */
    @NotNull
    private String name;

    /** 必填列表 */
    @NotEmpty
    private List<String> tags;

    /** 选填，不应进入 required */
    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
