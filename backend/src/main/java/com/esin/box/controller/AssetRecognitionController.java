package com.esin.box.controller;

import com.esin.box.config.UserContextHolder;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.service.AssetRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/asset/recognition")
public class AssetRecognitionController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp",
            "image/bmp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    @Autowired
    private AssetRecognitionService assetRecognitionService;

    /**
     * 识别资产图片
     *
     * @param file 上传的图片文件
     * @return 识别结果列表
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<AssetScanImageDTO>> recognizeImage(@RequestParam("file") MultipartFile file) {

        // 记录请求信息
        if (file != null) {
            log.info("收到图片识别请求 - 文件名: {}, 大小: {} bytes, 类型: {}",
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType());
        }

        // 验证文件
        String validationError = validateFile(file);
        if (validationError != null) {
            return Result.error(validationError);
        }

        // 获取当前用户
        String currentUser = UserContextHolder.getCurrentUsername();
        if (!StringUtils.hasText(currentUser)) {
            log.warn("图片识别请求被拒绝：用户未登录");
            return Result.error("用户未登录");
        }

        // 执行识别
        try {
            log.info("开始为用户 {} 执行图片识别", currentUser);
            long startTime = System.currentTimeMillis();

            List<AssetScanImageDTO> results = assetRecognitionService.recognizeAssetImage(file, currentUser);

            long endTime = System.currentTimeMillis();
            log.info("图片识别成功，用户: {}, 识别记录数: {}, 耗时: {}ms",
                    currentUser, results.size(), (endTime - startTime));

            // 根据结果数量设置不同的消息
            if (results.isEmpty()) {
                return Result.of(true, "未识别到有效的资产信息", results);
            } else {
                return Result.of(true, String.format("成功识别 %d 条资产记录", results.size()), results);
            }

        } catch (Exception e) {
            log.error("图片识别异常，用户: {}, 文件: {}", currentUser, file.getOriginalFilename(), e);

            // 根据异常类型返回不同的错误信息
            String errorMessage = getErrorMessage(e);
            return Result.error(errorMessage);
        }
    }

    /**
     * 验证上传的文件
     * @return 错误信息，如果验证通过返回null
     */
    private String validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return "请选择要上传的图片文件";
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            long sizeMB = file.getSize() / (1024 * 1024);
            return String.format("文件大小(%dMB)超过限制(10MB)", sizeMB);
        }

        // 检查文件类型（通过ContentType）
        String contentType = file.getContentType();
        if (contentType == null || !isImageContentType(contentType)) {
            return "文件类型不支持，请上传图片文件(支持: JPG/PNG/GIF/WEBP/BMP)";
        }

        // 检查文件扩展名（双重验证）
        String filename = file.getOriginalFilename();
        if (StringUtils.hasText(filename) && !hasValidImageExtension(filename)) {
            return "文件扩展名不正确，请上传图片文件";
        }

        return null;
    }

    /**
     * 检查是否为图片类型
     */
    private boolean isImageContentType(String contentType) {
        // 先检查精确匹配
        if (ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return true;
        }
        // 再检查是否以 image/ 开头
        return contentType.toLowerCase().startsWith("image/");
    }

    /**
     * 检查文件扩展名
     */
    private boolean hasValidImageExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowerCaseFilename::endsWith);
    }

    /**
     * 根据异常获取用户友好的错误信息
     */
    private String getErrorMessage(Exception e) {
        String message = e.getMessage();

        // 处理常见的异常情况
        if (e instanceof IllegalArgumentException) {
            return "参数错误: " + (message != null ? message : "请检查输入参数");
        }

        if (message != null) {
            if (message.contains("OCR")) {
                return "图片识别失败，请确保图片清晰可读";
            } else if (message.contains("timeout")) {
                return "图片识别超时，请稍后重试";
            } else if (message.contains("格式")) {
                return "图片格式不支持，请使用常见图片格式";
            } else if (message.contains("图片识别失败")) {
                // 如果已经是友好的错误信息，直接返回
                return message;
            }
        }

        // 默认错误信息
        return "图片识别失败，请检查图片质量后重试";
    }
}