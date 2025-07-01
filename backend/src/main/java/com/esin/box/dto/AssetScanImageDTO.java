package com.esin.box.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产扫描图像识别结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetScanImageDTO {

    // ========== 基础字段 ==========

    /**
     * 资产名称ID（这是关键字段）
     */
    private Long assetNameId;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 资产类型ID
     */
    private Long assetTypeId;

    /**
     * 单位ID
     */
    private Long unitId;

    /**
     * 资产位置ID
     */
    private Long assetLocationId;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 获取时间
     */
    private LocalDateTime acquireTime;

    /**
     * 备注
     */
    private String remark;

    // ========== OCR识别相关字段 ==========

    /**
     * 原始识别的名称
     */
    private String originalAssetName;

    /**
     * 清理后的名称
     */
    private String cleanedAssetName;

    /**
     * 匹配到的资产名称
     */
    private String matchedAssetName;

    /**
     * 匹配得分
     */
    private Double matchScore;

    /**
     * 是否匹配成功
     */
    private Boolean isMatched;

    // ========== 扩展字段 ==========

    /**
     * 识别置信度
     */
    private Double confidence;

    /**
     * 匹配的文本片段
     */
    private String matchedText;

    /**
     * 文本在图像中的位置信息
     */
    private String boundingBox;

    /**
     * 识别时间
     */
    private LocalDateTime recognitionTime;
}