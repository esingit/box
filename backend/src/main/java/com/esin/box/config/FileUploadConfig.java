package com.esin.box.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;  // 改为 jakarta
import java.io.File;

@Slf4j
@Configuration
public class FileUploadConfig {

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    @Value("${app.upload.path}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        createDirectory(tempDir, "临时文件");
        createDirectory(uploadPath, "上传文件");
    }

    private void createDirectory(String path, String description) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("{}目录创建成功: {}", description, path);
            } else {
                log.error("{}目录创建失败: {}", description, path);
            }
        } else {
            log.info("{}目录已存在: {}", description, path);
        }
    }
}