package com.esin.box.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import jakarta.annotation.PostConstruct;  // 改为 jakarta
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.ocr.tesseract")
public class TesseractConfig {

    private String dataPath;
    private String dataPathAlt;
    private String language = "chi_sim+eng";
    private int ocrEngineMode = 1;
    private int pageSegMode = 3;
    private int dpi = 300;
    private int timeout = 30;

    // Getters and setters
    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    public String getDataPathAlt() { return dataPathAlt; }
    public void setDataPathAlt(String dataPathAlt) { this.dataPathAlt = dataPathAlt; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getOcrEngineMode() { return ocrEngineMode; }
    public void setOcrEngineMode(int ocrEngineMode) { this.ocrEngineMode = ocrEngineMode; }
    public int getPageSegMode() { return pageSegMode; }
    public void setPageSegMode(int pageSegMode) { this.pageSegMode = pageSegMode; }
    public int getDpi() { return dpi; }
    public void setDpi(int dpi) { this.dpi = dpi; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    @PostConstruct
    public void autoDetectDataPath() {
        List<String> possiblePaths = Arrays.asList(
                "/usr/local/share/tessdata",           // Homebrew Intel
                "/opt/homebrew/share/tessdata",        // Homebrew Apple Silicon
                "/opt/local/share/tessdata",           // MacPorts
                "/usr/share/tesseract-ocr/4.00/tessdata", // Linux
                dataPath,                              // 配置的路径
                dataPathAlt                            // 备用路径
        );

        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                File chiSimFile = new File(path, "chi_sim.traineddata");
                File engFile = new File(path, "eng.traineddata");

                if (chiSimFile.exists() && engFile.exists()) {
                    this.dataPath = path;
                    log.info("Tesseract 数据路径自动检测成功: {}", path);
                    return;
                }
            }
        }

        log.warn("无法自动检测 Tesseract 数据路径，使用配置路径: {}", dataPath);
    }

    @Bean
    public ITesseract tesseract() {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(language);
        tesseract.setOcrEngineMode(ocrEngineMode);
        tesseract.setPageSegMode(pageSegMode);
        tesseract.setTessVariable("user_defined_dpi", String.valueOf(dpi));

        log.info("Tesseract 配置完成 - 数据路径: {}, 语言: {}", dataPath, language);
        return tesseract;
    }
}