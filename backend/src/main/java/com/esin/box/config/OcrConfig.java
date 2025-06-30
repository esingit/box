package com.esin.box.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "paddle.ocr")
public class OcrConfig {

    /**
     * Python环境路径
     */
    private String pythonPath = "/Users/ESin/Scripts/paddle_env/bin/python3";

    /**
     * Python脚本路径
     */
    private String scriptPath = "/Users/ESin/Scripts/paddle_ocr_processor.py";

    /**
     * 启动脚本路径（可选）
     */
    private String runScriptPath = "/Users/ESin/Scripts/run_ocr.sh";

    /**
     * 超时时间（秒）
     */
    private Long timeout = 60L;

    /**
     * 临时文件目录
     */
    private String tempDir = System.getProperty("java.io.tmpdir");

    /**
     * 支持的文件格式
     */
    private String[] supportedFormats = {"jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp"};

    /**
     * 最大文件大小（MB）
     */
    private Long maxFileSize = 10L;

    /**
     * 是否启用缓存
     */
    private Boolean enableCache = true;

    /**
     * 缓存过期时间（分钟）
     */
    private Long cacheExpireMinutes = 30L;

    /**
     * 是否使用启动脚本（推荐）
     */
    private Boolean useRunScript = true;
}