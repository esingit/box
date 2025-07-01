package com.esin.box.dto.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR文本识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrTextResult {

    /**
     * 文本ID
     */
    private Integer id;

    /**
     * 识别的文本内容
     */
    private String text;

    /**
     * 置信度 (0-1)
     */
    private Double confidence;

    /**
     * 边界框信息
     */
    private BoundingBox bbox;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private Double left;
        private Double top;
        private Double right;
        private Double bottom;
        private Double centerX;
        private Double centerY;
    }
}