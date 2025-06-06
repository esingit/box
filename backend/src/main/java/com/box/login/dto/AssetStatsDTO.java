package com.box.login.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AssetStatsDTO {
    private Double totalAssets;     // 总资产
    private Double totalLiabilities; // 总负债
    private String latestDate;      // 统计日期
    private String formattedDate;   // 格式化后的日期显示
}
