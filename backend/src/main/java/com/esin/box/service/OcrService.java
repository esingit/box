package com.esin.box.service;

import com.esin.box.dto.ocr.OcrRequestDTO;
import com.esin.box.dto.ocr.OcrResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * OCR识别服务接口
 */
public interface OcrService {

    /**
     * 识别上传的图像文件
     *
     * @param request OCR请求
     * @return OCR识别结果
     */
    OcrResponseDTO recognizeImage(OcrRequestDTO request);

    /**
     * 识别MultipartFile图像
     *
     * @param multipartFile 图像文件
     * @param language 识别语言
     * @return OCR识别结果
     */
    OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language);

    /**
     * 识别本地图像文件
     *
     * @param imageFile 图像文件
     * @param language 识别语言
     * @return OCR识别结果
     */
    OcrResponseDTO recognizeImage(File imageFile, String language);

    /**
     * 识别MultipartFile图像（带用户信息）
     *
     * @param multipartFile 图像文件
     * @param language 识别语言
     * @param username 用户名
     * @return OCR识别结果
     */
    OcrResponseDTO recognizeImage(MultipartFile multipartFile, String language, String username);
}