package com.esin.box.dto;

import com.esin.box.service.PaddleOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "OCR识别", description = "图片文字识别相关接口")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OcrController {

    private final PaddleOcrService paddleOcrService;

    @Operation(summary = "上传图片进行OCR识别", description = "支持jpg、png、bmp等常见图片格式")
    @PostMapping("/upload")
    public Result<OcrResponse> uploadAndOcr(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") @NotNull(message = "图片文件不能为空") MultipartFile file,

            @Parameter(description = "语言设置", example = "ch")
            @RequestParam(value = "lang", defaultValue = "ch") String lang,

            @Parameter(description = "最小置信度阈值", example = "0.7")
            @RequestParam(value = "minConfidence", defaultValue = "0.0")
            @DecimalMin(value = "0.0", message = "置信度不能小于0")
            @DecimalMax(value = "1.0", message = "置信度不能大于1")
            Double minConfidence) {

        try {
            log.info("收到OCR请求，文件名: {}, 大小: {} bytes, 语言: {}, 最小置信度: {}",
                    file.getOriginalFilename(), file.getSize(), lang, minConfidence);

            OcrResponse response = paddleOcrService.processUploadedFile(file, lang, minConfidence);

            if (response.getSuccess()) {
                log.info("OCR处理成功，任务ID: {}, 识别到 {} 个文本区域",
                        response.getTaskId(), response.getTotalTextRegions());
                return Result.success(response);
            } else {
                log.warn("OCR处理失败，任务ID: {}, 错误: {}", response.getTaskId(), response.getMessage());
                return Result.error("OCR识别失败: " + response.getMessage());
            }

        } catch (IllegalArgumentException e) {
            log.warn("OCR请求参数错误: {}", e.getMessage());
            return Result.error("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("OCR服务异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }

    @Operation(summary = "通过文件路径进行OCR识别", description = "适用于服务器本地文件")
    @PostMapping("/path")
    public Result<OcrResponse> ocrByPath(
            @Parameter(description = "图片文件路径", required = true)
            @RequestParam("path") @NotBlank(message = "文件路径不能为空") String imagePath,

            @Parameter(description = "语言设置", example = "ch")
            @RequestParam(value = "lang", defaultValue = "ch") String lang) {

        try {
            log.info("收到路径OCR请求，文件路径: {}, 语言: {}", imagePath, lang);

            OcrResponse response = paddleOcrService.processImageByPath(imagePath, lang);

            if (response.getSuccess()) {
                return Result.success(response);
            } else {
                return Result.error("OCR识别失败: " + response.getMessage());
            }

        } catch (Exception e) {
            log.error("路径OCR服务异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }

    @Operation(summary = "提取纯文本", description = "只返回识别出的文本内容，不包含位置信息")
    @PostMapping("/text")
    public Result<String> extractText(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") @NotNull(message = "图片文件不能为空") MultipartFile file) {

        try {
            String text = paddleOcrService.extractText(file);
            return Result.success(text);

        } catch (Exception e) {
            log.error("文本提取异常", e);
            return Result.error("文本提取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "提取高置信度文本", description = "只返回置信度高于指定阈值的文本")
    @PostMapping("/text/high-confidence")
    public Result<String> extractHighConfidenceText(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") @NotNull(message = "图片文件不能为空") MultipartFile file,

            @Parameter(description = "最小置信度阈值", example = "0.8")
            @RequestParam(value = "minConfidence", defaultValue = "0.8")
            @DecimalMin(value = "0.0", message = "置信度不能小于0")
            @DecimalMax(value = "1.0", message = "置信度不能大于1")
            Double minConfidence) {

        try {
            String text = paddleOcrService.extractHighConfidenceText(file, minConfidence);
            return Result.success(text);

        } catch (Exception e) {
            log.error("高置信度文本提取异常", e);
            return Result.error("文本提取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量OCR识别", description = "一次上传多个图片进行批量识别")
    @PostMapping("/batch")
    public Result<java.util.List<OcrResponse>> batchOcr(
            @Parameter(description = "图片文件列表", required = true)
            @RequestParam("files") @NotNull(message = "文件列表不能为空") MultipartFile[] files,

            @Parameter(description = "语言设置", example = "ch")
            @RequestParam(value = "lang", defaultValue = "ch") String lang,

            @Parameter(description = "最小置信度阈值", example = "0.7")
            @RequestParam(value = "minConfidence", defaultValue = "0.0")
            @DecimalMin(value = "0.0", message = "置信度不能小于0")
            @DecimalMax(value = "1.0", message = "置信度不能大于1")
            Double minConfidence) {

        try {
            if (files.length == 0) {
                return Result.error("至少需要上传一个文件");
            }

            if (files.length > 10) {
                return Result.error("批量处理最多支持10个文件");
            }

            log.info("收到批量OCR请求，文件数量: {}, 语言: {}", files.length, lang);

            java.util.List<OcrResponse> responses = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                OcrResponse response = paddleOcrService.processUploadedFile(file, lang, minConfidence);
                responses.add(response);
            }

            return Result.success(responses);

        } catch (Exception e) {
            log.error("批量OCR服务异常", e);
            return Result.error("批量OCR服务异常，请稍后重试");
        }
    }
}