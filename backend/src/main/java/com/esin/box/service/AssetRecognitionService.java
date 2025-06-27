package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.service.impl.recognition.RecognitionScheme;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AssetRecognitionService {

    /**
     * 指定方案进行图片识别
     */
    List<AssetScanImageDTO> recognizeWithScheme(MultipartFile image, String createUser, RecognitionScheme scheme) throws Exception;

    /**
     * 自动选择方案（兼容老接口）
     */
    @Deprecated
    List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception;

    /**
     * 自动识别（兼容老接口）
     */
    @Deprecated
    List<AssetScanImageDTO> recognizeImageAuto(MultipartFile image, String createUser) throws Exception;
}