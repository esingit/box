package com.esin.box.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchAddAssetRequest {
    /**
     * 资产记录列表
     */
    @NotEmpty(message = "记录列表不能为空")
    private List<AssetRecordDTO> records;

    /**
     * 是否强制覆盖（当今日已有记录时）
     */
    private boolean forceOverwrite = false;

    /**
     * 是否复制上回记录（当今日无记录时）
     */
    private boolean copyLast = false;
}

