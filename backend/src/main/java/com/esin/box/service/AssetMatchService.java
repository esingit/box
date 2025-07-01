package com.esin.box.service;

import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.dto.ocr.OcrTextResult;

import java.util.List;


/**
 * 资产匹配服务接口
 */
public interface AssetMatchService {

    /**
     * 从OCR结果中匹配资产信息
     *
     * @param ocrTexts OCR识别结果列表
     * @param username 用户名
     * @return 匹配的资产列表
     */
    List<AssetScanImageDTO> matchAssetsFromOcr(List<OcrTextResult> ocrTexts, String username);

    /**
     * 从文本列表中匹配资产信息
     *
     * @param extractedTexts 提取的文本列表
     * @param username 用户名
     * @return 匹配的资产列表
     */
    List<AssetScanImageDTO> matchAssetsFromTexts(List<String> extractedTexts, String username);

    /**
     * 匹配单个文本
     *
     * @param text 要匹配的文本
     * @param username 用户名
     * @return 匹配结果，如果没有匹配则返回null
     */
    AssetScanImageDTO matchSingleText(String text, String username);

    /**
     * 获取最佳匹配资产
     *
     * @param extractedTexts 提取的文本列表
     * @param username 用户名
     * @return 最佳匹配结果，如果没有找到则返回null
     */
    AssetScanImageDTO getBestMatch(List<String> extractedTexts, String username);
}