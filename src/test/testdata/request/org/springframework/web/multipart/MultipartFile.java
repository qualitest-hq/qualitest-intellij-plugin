package org.springframework.web.multipart;

import java.io.InputStream;

/**
 * 测试用 MultipartFile 接口桩（真实包名），供 Light Fixture 解析文件参数类型。
 */
public interface MultipartFile {

    String getName();

    String getOriginalFilename();

    String getContentType();

    boolean isEmpty();

    long getSize();

    byte[] getBytes();

    InputStream getInputStream();
}
