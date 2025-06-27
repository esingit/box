package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssetRecognitionService {
    List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception;
    List<AssetScanImageDTO> recognizeImageAuto(MultipartFile image, String createUser) throws Exception;
}
