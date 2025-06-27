package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AssetRecognitionService {

    /**
     * 智能识别图片 - 自动选择最佳方案
     */
    List<AssetScanImageDTO> recognizeImage(MultipartFile image, String createUser) throws Exception;
}