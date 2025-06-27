package com.esin.box.dto;

import com.esin.box.entity.AssetRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssetRecordDTO extends AssetRecord {
    private String assetName;   // 资产名称
    private String assetTypeName;    // 资产类型名称
    private String assetTypeValue;   // 资产类型值
    private String unitName;    // 货币单位名称
    private String unitValue;   // 货币单位值
    private String assetLocationName;  // 资产位置名称
    private String assetLocationValue; // 资产位置值
    private BigDecimal amount; //金额
    private LocalDateTime acquireTime;
}
