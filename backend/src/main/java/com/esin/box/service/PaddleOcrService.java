package com.esin.box.service;

import com.esin.box.config.OcrConfig;
import com.esin.box.dto.OcrResponse;
import com.esin.box.dto.OcrResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaddleOcrService {

    private final OcrConfig ocrConfig;
    private final ObjectMapper objectMapper;

    /**
     * 处理上传的图片文件
     */
    public OcrResponse processUploadedFile(MultipartFile file, String lang, Double minConfidence) {
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 验证文件
            validateFile(file);

            // 保存临时文件
            Path tempFile = saveTemporaryFile(file, taskId);

            try {
                // 执行OCR识别（带缓存）
                OcrResult result = processImageFileWithCache(tempFile.toString(), lang);

                // 过滤低置信度文本
                if (minConfidence > 0) {
                    result = filterByConfidence(result, minConfidence);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("OCR处理完成，任务ID: {}, 耗时: {}ms", taskId, duration);

                return OcrResponse.success(result, taskId, duration);

            } finally {
                // 清理临时文件
                cleanupTemporaryFile(tempFile);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OCR处理失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            return OcrResponse.failure(taskId, "OCR处理失败: " + e.getMessage(), duration);
        }
    }

    /**
     * 通过文件路径处理图片
     */
    public OcrResponse processImageByPath(String imagePath, String lang) {
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 验证文件路径
            Path imageFile = Paths.get(imagePath);
            if (!Files.exists(imageFile)) {
                throw new IllegalArgumentException("图片文件不存在: " + imagePath);
            }

            // 执行OCR识别（带缓存）
            OcrResult result = processImageFileWithCache(imagePath, lang);

            long duration = System.currentTimeMillis() - startTime;
            log.info("OCR处理完成，任务ID: {}, 文件: {}, 耗时: {}ms", taskId, imagePath, duration);

            return OcrResponse.success(result, taskId, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OCR处理失败，任务ID: {}, 文件: {}, 错误: {}", taskId, imagePath, e.getMessage(), e);
            return OcrResponse.failure(taskId, "OCR处理失败: " + e.getMessage(), duration);
        }
    }

    /**
     * 带缓存的OCR处理方法 - 这是缓存的入口点
     */
    @Cacheable(value = "ocr_results",
            key = "#imagePath + '_' + #lang",
            condition = "@ocrConfig.enableCache",
            unless = "#result == null || !#result.success")
    public OcrResult processImageFileWithCache(String imagePath, String lang) throws Exception {
        log.debug("执行OCR处理（无缓存命中）: {}, 语言: {}", imagePath, lang);
        return executeOcrProcess(imagePath, lang);
    }

    /**
     * 不带缓存的OCR处理方法 - 用于不需要缓存的场景
     */
    public OcrResult processImageFileWithoutCache(String imagePath, String lang) throws Exception {
        log.debug("执行OCR处理（不使用缓存）: {}, 语言: {}", imagePath, lang);
        return executeOcrProcess(imagePath, lang);
    }

    /**
     * 清除指定文件的缓存
     */
    @CacheEvict(value = "ocr_results", key = "#imagePath + '_' + #lang")
    public void evictCache(String imagePath, String lang) {
        log.debug("清除OCR缓存: {}, 语言: {}", imagePath, lang);
    }

    /**
     * 清除所有OCR缓存
     */
    @CacheEvict(value = "ocr_results", allEntries = true)
    public void clearAllCache() {
        log.info("清除所有OCR缓存");
    }

    /**
     * 提取纯文本（兼容旧接口）
     */
    public String extractText(MultipartFile file) {
        OcrResponse response = processUploadedFile(file, "ch", 0.0);
        return response.getSuccess() ? response.getExtractedText() : "";
    }

    /**
     * 获取高置信度文本
     */
    public String extractHighConfidenceText(MultipartFile file, double minConfidence) {
        OcrResponse response = processUploadedFile(file, "ch", minConfidence);
        return response.getSuccess() ? response.getSortedText() : "";
    }

    /**
     * 执行OCR处理的核心逻辑（私有方法，不带缓存注解）
     */
    private OcrResult executeOcrProcess(String imagePath, String lang) throws Exception {

        // 构建命令
        ProcessBuilder processBuilder;

        if (ocrConfig.getUseRunScript() && Files.exists(Paths.get(ocrConfig.getRunScriptPath()))) {
            // 使用启动脚本（推荐）
            processBuilder = new ProcessBuilder(
                    ocrConfig.getRunScriptPath(),
                    "--image", imagePath,
                    "--lang", lang
            );
        } else {
            // 直接使用Python
            processBuilder = new ProcessBuilder(
                    ocrConfig.getPythonPath(),
                    ocrConfig.getScriptPath(),
                    "--image", imagePath,
                    "--lang", lang
            );
        }

        processBuilder.redirectErrorStream(true);

        log.debug("执行OCR命令: {}", String.join(" ", processBuilder.command()));

        // 执行命令
        Process process = processBuilder.start();

        // 读取输出
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"));
             BufferedReader errorReader = new BufferedReader(
                     new InputStreamReader(process.getErrorStream(), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        // 等待进程完成
        boolean finished = process.waitFor(ocrConfig.getTimeout(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("OCR处理超时，超过 " + ocrConfig.getTimeout() + " 秒");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("OCR进程异常退出，退出码: {}, 错误输出: {}", exitCode, errorOutput.toString());
            throw new RuntimeException("OCR进程异常退出，退出码: " + exitCode);
        }

        // 解析JSON结果
        String jsonOutput = output.toString().trim();
        log.debug("OCR原始输出长度: {} 字符", jsonOutput.length());

        if (jsonOutput.isEmpty()) {
            throw new RuntimeException("OCR进程没有返回任何输出");
        }

        try {
            OcrResult result = objectMapper.readValue(jsonOutput, OcrResult.class);

            if (result.getSuccess()) {
                log.debug("OCR处理成功，识别到 {} 个文本区域",
                        result.getData() != null ? result.getData().getTotalTextRegions() : 0);
            } else {
                log.warn("OCR处理失败: {}", result.getError());
            }

            return result;

        } catch (Exception e) {
            log.error("解析OCR结果失败，输出前100字符: {}",
                    jsonOutput.length() > 100 ? jsonOutput.substring(0, 100) + "..." : jsonOutput, e);
            throw new RuntimeException("解析OCR结果失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > ocrConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小不能超过 " + ocrConfig.getMaxFileSize() + "MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片文件");
        }

        // 检查文件扩展名
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String fileExtension = getFileExtension(fileName);
        if (!Arrays.asList(ocrConfig.getSupportedFormats()).contains(fileExtension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式: " + fileExtension +
                    "，支持的格式: " + String.join(", ", ocrConfig.getSupportedFormats()));
        }
    }

    /**
     * 保存临时文件
     */
    private Path saveTemporaryFile(MultipartFile file, String taskId) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String tempFileName = "ocr_" + taskId + "_" + System.currentTimeMillis() + "." + fileExtension;

        Path tempDir = Paths.get(ocrConfig.getTempDir());
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path tempFile = tempDir.resolve(tempFileName);
        Files.copy(file.getInputStream(), tempFile);

        log.debug("临时文件已保存: {}", tempFile.toString());
        return tempFile;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTemporaryFile(Path tempFile) {
        try {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
                log.debug("临时文件已删除: {}", tempFile.toString());
            }
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", tempFile.toString(), e);
        }
    }

    /**
     * 根据置信度过滤结果
     */
    private OcrResult filterByConfidence(OcrResult result, Double minConfidence) {
        if (result.getData() == null || result.getData().getTextRegions() == null) {
            return result;
        }

        // 过滤低置信度的文本区域
        var filteredRegions = result.getData().getTextRegions().stream()
                .filter(region -> region.getConfidence() >= minConfidence)
                .toList();

        // 更新数据
        result.getData().setTextRegions(filteredRegions);
        result.getData().setTotalTextRegions(filteredRegions.size());

        // 重新计算文本
        String filteredText = filteredRegions.stream()
                .map(OcrResult.TextRegion::getText)
                .reduce("", (a, b) -> a + "\n" + b);

        result.getData().setFullText(filteredText);
        result.getData().setSortedFullText(filteredText);

        return result;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}