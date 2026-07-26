package com.qualitest.test.request;

/**
 * 普通 JSON 请求体 DTO，用于确认非 multipart 扫描仍为 json body。
 */
public class JsonBodyDto {

    private String name;

    private int count;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
