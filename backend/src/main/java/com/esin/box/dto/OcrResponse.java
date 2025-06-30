package com.esin.box.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {

    private String taskId;
    private Boolean success;
    private String message;
    private LocalDateTime processTime;
    private Long processingDuration; // 处理时间，毫秒

    // 基础结果
    private String extractedText;
    private String sortedText;
    private Integer totalTextRegions;

    // 统计信息
    private Double averageConfidence;
    private Integer totalCharacters;
    private Integer totalWords;

    // 详细结果
    private List<OcrResult.TextRegion> textRegions;
    private OcrResult.ConfidenceStats confidenceStats;

    // 原始结果（可选）
    private OcrResult rawResult;

    public static OcrResponse success(OcrResult ocrResult, String taskId, long duration) {
        if (!ocrResult.getSuccess() || ocrResult.getData() == null) {
            return failure(taskId, ocrResult.getError(), duration);
        }

        OcrResult.OcrData data = ocrResult.getData();
        return OcrResponse.builder()
                .taskId(taskId)
                .success(true)
                .message("OCR处理成功")
                .processTime(LocalDateTime.now())
                .processingDuration(duration)
                .extractedText(data.getFullText())
                .sortedText(data.getSortedFullText())
                .totalTextRegions(data.getTotalTextRegions())
                .averageConfidence(data.getConfidenceStats().getAverageConfidence())
                .totalCharacters(data.getProcessingInfo().getTotalCharacters())
                .totalWords(data.getProcessingInfo().getTotalWords())
                .textRegions(data.getTextRegions())
                .confidenceStats(data.getConfidenceStats())
                .rawResult(ocrResult)
                .build();
    }

    public static OcrResponse failure(String taskId, String errorMessage, long duration) {
        return OcrResponse.builder()
                .taskId(taskId)
                .success(false)
                .message(errorMessage)
                .processTime(LocalDateTime.now())
                .processingDuration(duration)
                .extractedText("")
                .sortedText("")
                .totalTextRegions(0)
                .averageConfidence(0.0)
                .totalCharacters(0)
                .totalWords(0)
                .build();
    }
}