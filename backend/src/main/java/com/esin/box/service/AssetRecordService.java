package com.esin.box.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.entity.AssetRecord;

import java.util.List;

public interface AssetRecordService {
    void addRecord(AssetRecord record);
    void updateRecord(AssetRecord record);
    void deleteRecord(Long id);
    IPage<AssetRecordDTO> pageByConditions(Page<AssetRecord> page, List<Long> assetNameIdList, List<Long> assetLocationIdList,
                                           List<Long> assetTypeIdList, String remark, String startDate, String endDate,
                                           String createUser);
    void copyLastRecords(boolean force);
    AssetStatsDTO getLatestStats(String createUser, Integer offset);
}
