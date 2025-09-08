package com.esin.box.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * PaddleOCR配置类
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app.ocr.paddle")
@ConditionalOnProperty(prefix = "app.ocr", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PaddleOcrConfig {

    /**
     * Python解释器路径
     */
    private String pythonPath = "python3";

    /**
     * PaddleOCR Python脚本路径
     */
    private String scriptPath;

    /**
     * 支持的语言
     */
    private String language = "ch";

    /**
     * OCR识别超时时间（秒）
     */
    private int timeout = 60;

    /**
     * 是否保留识别后的临时文件
     */
    private boolean keepTempFiles = false;

    /**
     * 最大并发处理数
     */
    private int maxConcurrent = 3;

    /**
     * 输出文件临时目录
     */
    private String outputTempDir;

    @PostConstruct
    public void init() {
        // 检查脚本路径是否存在
        if (scriptPath != null && !Files.exists(Paths.get(scriptPath))) {
            throw new IllegalArgumentException("PaddleOCR script not found: " + scriptPath);
        }

        // 创建临时输出目录
        if (outputTempDir != null) {
            File dir = new File(outputTempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
}