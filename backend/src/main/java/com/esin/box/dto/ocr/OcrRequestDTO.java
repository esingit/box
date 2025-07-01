package com.esin.box.dto.ocr;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


/**
 * OCR识别请求DTO
 */
@Data
public class OcrRequestDTO {

    /**
     * 上传的图像文件
     */
    @NotNull(message = "图像文件不能为空")
    private MultipartFile imageFile;

    /**
     * 识别语言（可选，默认使用配置）
     */
    private String language;

    /**
     * 是否保留临时文件（调试用）
     */
    private Boolean keepTempFiles;
}