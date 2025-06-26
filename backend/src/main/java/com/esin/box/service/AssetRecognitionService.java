package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资产识别服务接口
 */
public interface AssetRecognitionService {

    /**
     * 识别资产图片并匹配资产名称
     *
     * @param image 上传的图片文件
     * @param createUser 创建用户
     * @return 识别出的资产记录列表
     * @throws Exception 识别异常
     */
    List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception;
}