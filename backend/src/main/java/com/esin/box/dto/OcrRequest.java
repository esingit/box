package com.esin.box.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class OcrRequest {

    @NotNull(message = "图片文件不能为空")
    private MultipartFile file;

    @NotBlank(message = "语言设置不能为空")
    private String lang = "ch";

    private Double minConfidence = 0.0;

    private Boolean enableDebug = false;
}