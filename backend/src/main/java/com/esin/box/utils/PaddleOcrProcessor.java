package com.esin.box.utils;

import com.esin.box.config.PaddleOcrConfig;
import com.esin.box.dto.ocr.OcrResponseDTO;
import com.esin.box.dto.ocr.OcrImageInfo;
import com.esin.box.dto.ocr.OcrMetadata;
import com.esin.box.dto.ocr.OcrTextResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PaddleOCR处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaddleOcrProcessor {

    private final PaddleOcrConfig config;
    private final ObjectMapper objectMapper;

    /**
     * 处理图像并返回OCR结果
     */
    public OcrResponseDTO processImage(File imageFile, String language) throws Exception {
        // 创建输出文件
        File outputFile = createOutputFile();

        try {
            // 检查输入文件
            if (!imageFile.exists()) {
                throw new RuntimeException("输入图像文件不存在: " + imageFile.getAbsolutePath());
            }

            log.info("开始处理图像: 输入={}, 输出={}, 语言={}",
                    imageFile.getAbsolutePath(), outputFile.getAbsolutePath(), language);

            // 执行Python脚本
            boolean success = executePythonScript(imageFile, outputFile, language);

            if (!success) {
                throw new RuntimeException("PaddleOCR Python脚本执行失败");
            }

            // 解析结果
            return parseOcrResult(outputFile);

        } finally {
            // 清理输出文件
            if (outputFile.exists() && !config.isKeepTempFiles()) {
                try {
                    FileUtils.forceDelete(outputFile);
                } catch (IOException e) {
                    log.warn("删除输出文件失败: {}", outputFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 执行Python脚本
     */
    private boolean executePythonScript(File imageFile, File outputFile, String language) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(config.getPythonPath());
        command.add(config.getScriptPath());
        command.add("--image");
        command.add(imageFile.getAbsolutePath());
        command.add("--output");
        command.add(outputFile.getAbsolutePath());
        command.add("--lang");
        command.add(StringUtils.isNotBlank(language) ? language : config.getLanguage());

        log.info("执行命令: {}", String.join(" ", command));

        // 检查Python脚本是否存在
        File scriptFile = new File(config.getScriptPath());
        if (!scriptFile.exists()) {
            throw new RuntimeException("Python脚本文件不存在: " + config.getScriptPath());
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false); // 分别处理标准输出和错误输出

        Process process = pb.start();

        // 读取标准输出和错误输出
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        // 读取标准输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Python stdout: {}", line);
            }
        }

        // 读取错误输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
                log.error("Python stderr: {}", line);
            }
        }

        boolean finished = process.waitFor(config.getTimeout(), TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("PaddleOCR处理超时（" + config.getTimeout() + "秒）");
        }

        int exitCode = process.exitValue();

        log.info("Python脚本执行完成: 退出码={}, 输出长度={}, 错误长度={}",
                exitCode, output.length(), errorOutput.length());

        if (exitCode != 0) {
            String errorMsg = String.format(
                    "PaddleOCR处理失败，退出码: %d\n" +
                            "命令: %s\n" +
                            "标准输出: %s\n" +
                            "错误输出: %s\n" +
                            "输入文件: %s\n" +
                            "输出文件: %s",
                    exitCode,
                    String.join(" ", command),
                    output.toString().trim(),
                    errorOutput.toString().trim(),
                    imageFile.getAbsolutePath(),
                    outputFile.getAbsolutePath()
            );

            log.error("详细错误信息: {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 检查输出文件是否生成
        if (!outputFile.exists()) {
            throw new RuntimeException("Python脚本执行成功但未生成输出文件: " + outputFile.getAbsolutePath());
        }

        log.info("Python脚本执行成功，输出文件大小: {} bytes", outputFile.length());
        return true;
    }

    /**
     * 解析OCR结果
     */
    private OcrResponseDTO parseOcrResult(File outputFile) throws IOException {
        if (!outputFile.exists()) {
            throw new RuntimeException("OCR输出文件不存在");
        }

        String jsonContent = FileUtils.readFileToString(outputFile, "UTF-8");
        log.debug("OCR输出内容: {}", jsonContent);

        JsonNode rootNode = objectMapper.readTree(jsonContent);

        boolean success = rootNode.path("success").asBoolean(false);

        if (!success) {
            String error = rootNode.path("error").asText("未知错误");
            return OcrResponseDTO.builder()
                    .success(false)
                    .error(error)
                    .build();
        }

        // 解析元数据
        OcrMetadata metadata = parseMetadata(rootNode.path("metadata"));

        // 解析文本结果
        List<OcrTextResult> rawTexts = parseRawTexts(rootNode.path("raw_texts"));

        // 提取纯文本
        List<String> extractedTexts = rawTexts.stream()
                .map(OcrTextResult::getText)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        // 合并完整文本
        String fullText = String.join("\n", extractedTexts);

        return OcrResponseDTO.builder()
                .success(true)
                .metadata(metadata)
                .rawTexts(rawTexts)
                .extractedTexts(extractedTexts)
                .fullText(fullText)
                .build();
    }

    /**
     * 解析元数据
     */
    private OcrMetadata parseMetadata(JsonNode metadataNode) {
        if (metadataNode.isMissingNode()) {
            return null;
        }

        JsonNode imageInfoNode = metadataNode.path("image_info");
        OcrImageInfo imageInfo = null;
        if (!imageInfoNode.isMissingNode()) {
            imageInfo = OcrImageInfo.builder()
                    .width(imageInfoNode.path("width").asDouble())
                    .height(imageInfoNode.path("height").asDouble())
                    .build();
        }

        return OcrMetadata.builder()
                .totalTexts(metadataNode.path("total_texts").asInt())
                .processTime(metadataNode.path("process_time").asDouble())
                .timestamp(metadataNode.path("timestamp").asText())
                .imageInfo(imageInfo)
                .build();
    }

    /**
     * 解析文本结果
     */
    private List<OcrTextResult> parseRawTexts(JsonNode rawTextsNode) {
        List<OcrTextResult> results = new ArrayList<>();

        if (rawTextsNode.isArray()) {
            for (JsonNode textNode : rawTextsNode) {
                OcrTextResult.BoundingBox bbox = null;
                JsonNode bboxNode = textNode.path("bbox");
                if (!bboxNode.isMissingNode()) {
                    bbox = OcrTextResult.BoundingBox.builder()
                            .left(bboxNode.path("left").asDouble())
                            .top(bboxNode.path("top").asDouble())
                            .right(bboxNode.path("right").asDouble())
                            .bottom(bboxNode.path("bottom").asDouble())
                            .centerX(bboxNode.path("center_x").asDouble())
                            .centerY(bboxNode.path("center_y").asDouble())
                            .build();
                }

                OcrTextResult result = OcrTextResult.builder()
                        .id(textNode.path("id").asInt())
                        .text(textNode.path("text").asText())
                        .confidence(textNode.path("confidence").asDouble())
                        .bbox(bbox)
                        .build();

                results.add(result);
            }
        }

        return results;
    }

    /**
     * 创建输出文件
     */
    private File createOutputFile() throws IOException {
        String tempDir = config.getOutputTempDir();
        if (StringUtils.isBlank(tempDir)) {
            tempDir = System.getProperty("java.io.tmpdir");
        }

        Path outputPath = Paths.get(tempDir, "paddle_ocr_output_" + UUID.randomUUID() + ".json");
        File outputFile = outputPath.toFile();

        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        return outputFile;
    }
}