package com.esin.box.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.dto.BatchAddAssetRequest;
import com.esin.box.dto.BatchAddResult;
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

    List<AssetRecordDTO> listByConditions(List<Long> assetNameIdList,
                                          List<Long> assetLocationIdList,
                                          List<Long> assetTypeIdList,
                                          String remark,
                                          String startDate,
                                          String endDate,
                                          String createUser);

    /**
     * 检查指定用户今日是否有记录
     */
    boolean hasTodayRecords(String username);

    /**
     * 智能批量添加资产记录
     *
     * @param records        识别的资产记录列表
     * @param forceOverwrite 是否强制覆盖（当今日已有记录时）
     * @param copyLast       是否复制上回记录（当今日无记录时）
     * @param createUser     创建用户
     * @return 批量添加结果
     */
    BatchAddResult smartBatchAddRecords(List<AssetRecordDTO> records, boolean forceOverwrite, boolean copyLast, String createUser);
}
