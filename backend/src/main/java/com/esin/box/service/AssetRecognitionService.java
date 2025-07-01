package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资产识别服务接口
 */
public interface AssetRecognitionService {

    /**
     * 识别图片中的资产信息
     *
     * @param file 图片文件
     * @param username 用户名
     * @return 识别到的资产列表
     */
    List<AssetScanImageDTO> recognizeImage(MultipartFile file, String username);
}