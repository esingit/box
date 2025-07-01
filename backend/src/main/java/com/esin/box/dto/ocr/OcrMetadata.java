package com.esin.box.dto.ocr;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OCR元数据信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrMetadata {

    /**
     * 识别到的文本总数
     */
    private Integer totalTexts;

    /**
     * 处理耗时（秒）
     */
    private Double processTime;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 图像信息
     */
    private OcrImageInfo imageInfo;
}