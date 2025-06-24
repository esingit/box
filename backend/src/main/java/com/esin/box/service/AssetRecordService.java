package com.esin.box.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.entity.AssetRecord;
import org.springframework.web.multipart.MultipartFile;

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
     * 识别图片中的资产信息
     * @param image 图片文件
     * @return 识别结果列表
     */
    List<AssetRecordDTO> recognizeAssetImage(MultipartFile image) throws Exception;

    /**
     * 批量添加资产记录
     * @param records 资产记录列表
     * @param createUser 创建用户
     * @return 成功添加的记录数
     */
    int batchAddRecords(List<AssetRecordDTO> records, String createUser);
}
