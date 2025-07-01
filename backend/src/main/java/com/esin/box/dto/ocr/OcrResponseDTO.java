package com.esin.box.dto.ocr;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * OCR识别响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponseDTO {

    /**
     * 识别是否成功
     */
    private Boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String error;

    /**
     * 元数据信息
     */
    private OcrMetadata metadata;

    /**
     * 识别的文本结果列表
     */
    private List<OcrTextResult> rawTexts;

    /**
     * 提取的纯文本（按行分割）
     */
    private List<String> extractedTexts;

    /**
     * 合并的完整文本
     */
    private String fullText;
}