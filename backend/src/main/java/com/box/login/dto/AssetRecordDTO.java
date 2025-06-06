package com.box.login.dto;

import com.box.login.entity.AssetRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssetRecordDTO extends AssetRecord {
    private String assetName;   // 资产名称
    private String assetTypeName;    // 资产类型名称
    private String assetTypeValue;   // 资产类型值
    private String unitName;    // 货币单位名称
    private String unitValue;   // 货币单位值
    private String locationName;  // 资产位置名称
    private String locationValue; // 资产位置值
}
