package com.esin.box.service.impl;

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

    private final PaddleOcrProcessor processor;

    @Override
    public OcrResponseDTO recognizeImage(OcrRequestDTO request) {
        return OcrResponseDTO.builder()
                .success(false)
                .error("OCR功能已被禁用")
                .build();
    }

    @Override
    public OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language) {
        return OcrResponseDTO.builder()
                .success(false)
                .error("OCR功能已被禁用")
                .build();
    }

    @Override
    public OcrResponseDTO recognizeImage(File imageFile, String language) {
        return OcrResponseDTO.builder()
                .success(false)
                .error("OCR功能已被禁用")
                .build();
    }

    @Override
    public OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language, String username) {
        return OcrResponseDTO.builder()
                .success(false)
                .error("OCR功能已被禁用")
                .build();
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

        String tempDir = System.getProperty("java.io.tmpdir");

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
        if (tempFile != null && tempFile.exists()) {
            try {
                FileUtils.forceDelete(tempFile);
                log.debug("删除临时文件: {}, 用户: {}", tempFile.getAbsolutePath(), username);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}, 用户: {}", tempFile.getAbsolutePath(), username, e);
            }
        }
    }
}
