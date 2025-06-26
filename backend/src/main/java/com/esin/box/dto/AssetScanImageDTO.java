package com.esin.box.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ScanImageDTO.java - 扫描图片记录类
@Data
public class AssetScanImageDTO {
    // 基础字段
    private Long assetNameId;           // 资产名称ID（这是关键字段）
    private String assetName;
    private Long assetTypeId;
    private Long unitId;
    private Long assetLocationId;
    private BigDecimal amount;          // 金额
    private LocalDateTime acquireTime;
    private String remark;
    // OCR识别相关字段
    private String originalAssetName;    // 原始识别的名称
    private String cleanedAssetName;     // 清理后的名称
    private String matchedAssetName;     // 匹配到的资产名称
    private Double matchScore;           // 匹配得分
    private Boolean isMatched;           // 是否匹配成功
}