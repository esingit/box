package com.esin.box.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetStatsDTO {
    /**
     * 净资产（总资产-总负债）
     */
    private Double netAssets;

    /**
     * 总负债
     */
    private Double totalLiabilities;

    /**
     * 理财资产总和（基金+理财+股票）
     */
    private Double investmentAssets;

    /**
     * 最新记录日期
     */
    private String latestDate;

    /**
     * 格式化的日期显示
     */
    private String formattedDate;

    /**
     * 净资产变化
     */
    private Double netAssetsChange;

    /**
     * 负债变化
     */
    private Double liabilitiesChange;

    /**
     * 理财资产变化
     */
    private Double investmentAssetsChange;
}