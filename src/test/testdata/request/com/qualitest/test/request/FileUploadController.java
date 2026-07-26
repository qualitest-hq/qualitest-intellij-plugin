package com.qualitest.test.request;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 请求扫描测试用 Controller：覆盖文件上传与 JSON 请求体的常见写法。
 */
public class FileUploadController {

    /** RequestPart 文件 + RequestParam 文本字段 */
    @PostMapping(value = "/upload-part", consumes = "multipart/form-data")
    public String uploadByRequestPart(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "bizType", required = false) String bizType) {
        return "ok";
    }

    /** RequestParam 绑定的 MultipartFile */
    @PostMapping(value = "/upload-param", consumes = "multipart/form-data")
    public String uploadByRequestParam(@RequestParam("file") MultipartFile file) {
        return "ok";
    }

    /** ModelAttribute 表单对象（内含文件字段与文本字段） */
    @PostMapping(value = "/upload-model", consumes = "multipart/form-data")
    public String uploadByModelAttribute(@ModelAttribute UploadForm form) {
        return "ok";
    }

    /** RequestBody + multipart，DTO 字段含文件与文本 */
    @PostMapping(value = "/upload-body", consumes = "multipart/form-data")
    public String uploadByRequestBody(@RequestBody UploadForm form) {
        return "ok";
    }

    /** 多文件：List&lt;MultipartFile&gt; */
    @PostMapping(value = "/upload-list", consumes = "multipart/form-data")
    public String uploadList(@RequestPart("files") List<MultipartFile> files) {
        return "ok";
    }

    /** 纯 JSON 请求体，扫描结果应为 json body */
    @PostMapping("/json")
    public String jsonBody(@RequestBody JsonBodyDto body) {
        return "ok";
    }
}
