package com.esin.box.service.impl;

import com.esin.box.config.PaddleOcrConfig;
import com.esin.box.dto.ocr.OcrRequestDTO;
import com.esin.box.dto.ocr.OcrResponseDTO;
import com.esin.box.service.OcrService;
import com.esin.box.utils.PaddleOcrProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


/**
 * PaddleOCR服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaddleOcrServiceImpl implements OcrService {

    private final PaddleOcrConfig config;
    private final PaddleOcrProcessor processor;

    @Override
    public OcrResponseDTO recognizeImage(OcrRequestDTO request) {
        // 委托给带用户信息的方法
        return recognizeImage(request.getImageFile(), request.getLanguage(), null);
    }

    @Override
    public OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language) {
        return recognizeImage(multipartFile, language, null);
    }

    @Override
    public OcrResponseDTO recognizeImage(File imageFile, String language) {
        log.info("开始识别图像文件: {}", imageFile.getAbsolutePath());

        try {
            // 使用提供的语言或配置的默认语言
            String lang = StringUtils.isNotBlank(language) ? language : config.getLanguage();

            // 调用PaddleOCR处理器
            OcrResponseDTO result = processor.processImage(imageFile, lang);

            log.info("图像识别完成: 成功={}, 文本数量={}",
                    result.getSuccess(),
                    result.getMetadata() != null ? result.getMetadata().getTotalTexts() : 0);

            return result;

        } catch (Exception e) {
            log.error("图像识别失败: {}", e.getMessage(), e);
            return OcrResponseDTO.builder()
                    .success(false)
                    .error("OCR识别失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language, String username) {
        if (multipartFile.isEmpty()) {
            return OcrResponseDTO.builder()
                    .success(false)
                    .error("上传的文件为空")
                    .build();
        }

        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = createTempFile(multipartFile, username);

            log.info("开始OCR识别: 文件={}, 用户={}, 语言={}",
                    multipartFile.getOriginalFilename(), username, language);

            // 识别图像
            OcrResponseDTO result = recognizeImage(tempFile, language);

            // 记录识别结果
            if (result.getSuccess()) {
                int textCount = result.getMetadata() != null ? result.getMetadata().getTotalTexts() : 0;
                log.info("OCR识别成功: 用户={}, 文本数量={}", username, textCount);
            } else {
                log.warn("OCR识别失败: 用户={}, 错误={}", username, result.getError());
            }

            return result;

        } catch (IOException e) {
            log.error("创建临时文件失败: 用户={}, 错误={}", username, e.getMessage(), e);
            return OcrResponseDTO.builder()
                    .success(false)
                    .error("文件处理失败: " + e.getMessage())
                    .build();
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile, username);
        }
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(MultipartFile multipartFile, String username) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String tempDir = config.getOutputTempDir();
        if (StringUtils.isBlank(tempDir)) {
            tempDir = System.getProperty("java.io.tmpdir");
        }

        // 包含用户信息的临时文件名
        String fileName = String.format("paddle_ocr_%s_%s%s",
                StringUtils.isNotBlank(username) ? username : "unknown",
                UUID.randomUUID().toString().substring(0, 8),
                extension);

        Path tempPath = Paths.get(tempDir, fileName);
        File tempFile = tempPath.toFile();

        // 确保父目录存在
        File parentDir = tempFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 保存文件
        multipartFile.transferTo(tempFile);

        log.debug("创建临时文件: {}, 用户: {}", tempFile.getAbsolutePath(), username);
        return tempFile;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile, String username) {
        if (tempFile != null && tempFile.exists() && !config.isKeepTempFiles()) {
            try {
                FileUtils.forceDelete(tempFile);
                log.debug("删除临时文件: {}, 用户: {}", tempFile.getAbsolutePath(), username);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}, 用户: {}", tempFile.getAbsolutePath(), username, e);
            }
        }
    }
}

