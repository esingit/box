package com.esin.box.utils;

import com.esin.box.dto.ocr.OcrResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * PaddleOCR处理器
 */
@Slf4j
@Component
public class PaddleOcrProcessor {

    private final ObjectMapper objectMapper;

    public PaddleOcrProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理图像并返回OCR结果
     */
    public OcrResponseDTO processImage(File imageFile, String language) throws Exception {
        // 由于OCR功能已禁用，直接返回失败结果
        return OcrResponseDTO.builder()
                .success(false)
                .error("OCR功能已被禁用")
                .build();
    }

    /**
     * 执行Python脚本
     */
    private boolean executePythonScript(File imageFile, File outputFile, String language) throws Exception {
        // OCR功能已禁用，不执行任何命令，直接抛出异常或返回false
        throw new RuntimeException("OCR功能已被禁用，不执行Python脚本");
    }
}