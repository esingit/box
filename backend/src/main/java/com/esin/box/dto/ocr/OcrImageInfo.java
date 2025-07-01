package com.esin.box.dto.ocr;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OCR图像信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrImageInfo {

    /**
     * 图像宽度
     */
    private Double width;

    /**
     * 图像高度
     */
    private Double height;
}