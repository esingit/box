package com.esin.box.dto.ocr;

import com.esin.box.entity.AssetName;
import lombok.Data;

/**
 * 资产匹配结果
 */
@Data
public class AssetMatchResult {
    private AssetName assetName;
    private double score;
}