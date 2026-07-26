package com.qualitest.test.request;

import org.springframework.web.multipart.MultipartFile;

/**
 * 含文件字段与文本字段的表单对象，供 ModelAttribute / multipart RequestBody 扫描使用。
 */
public class UploadForm {

    /** 上传文件 */
    private MultipartFile file;

    /** 业务分类 */
    private String bizType;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }
}
