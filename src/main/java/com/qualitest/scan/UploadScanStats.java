package com.qualitest.scan;

import lombok.Builder;
import lombok.Value;

/**
 * 上传扫描统计：全量与带分组注释的 API / Controller 数量。
 */
@Value
@Builder
public class UploadScanStats {

    int totalApiCount;
    int totalControllerCount;
    int explicitApiCount;
    int explicitControllerCount;
}
