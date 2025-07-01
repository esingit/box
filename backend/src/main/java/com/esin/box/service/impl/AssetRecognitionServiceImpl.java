package com.esin.box.service.impl;

import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.dto.ocr.OcrResponseDTO;
import com.esin.box.service.AssetMatchService;
import com.esin.box.service.AssetRecognitionService;
import com.esin.box.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * 资产识别服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetRecognitionServiceImpl implements AssetRecognitionService {

    private final OcrService ocrService;
    private final AssetMatchService assetMatchService;

    @Override
    public List<AssetScanImageDTO> recognizeImage(MultipartFile file, String username) {
        log.info("开始资产识别: 文件={}, 用户={}", file.getOriginalFilename(), username);

        try {
            // 1. 执行OCR识别
            OcrResponseDTO ocrResult = ocrService.recognizeImage(file, "ch", username);

            if (!ocrResult.getSuccess()) {
                log.warn("OCR识别失败: 用户={}, 错误={}", username, ocrResult.getError());
                return Collections.emptyList();
            }

            // 2. 执行资产匹配
            List<AssetScanImageDTO> matchedAssets = assetMatchService.matchAssetsFromOcr(
                    ocrResult.getRawTexts(), username);

            log.info("资产识别完成: 用户={}, OCR文本={}, 匹配资产={}",
                    username,
                    ocrResult.getMetadata() != null ? ocrResult.getMetadata().getTotalTexts() : 0,
                    matchedAssets.size());

            return matchedAssets;

        } catch (Exception e) {
            log.error("资产识别异常: 用户={}, 文件={}", username, file.getOriginalFilename(), e);
            throw new RuntimeException("资产识别失败: " + e.getMessage(), e);
        }
    }
}