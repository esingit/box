package com.esin.box.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AssetDetailDTO {
    private String date;              // 统计日期
    private Long assetTypeId;         // 资金类型ID
    private String assetTypeName;     // 资金类型名称
    private Long assetNameId;         // 资金名称ID
    private String assetName;         // 资金名称
    private Double amount;            // 金额
}
