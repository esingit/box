package com.box.login.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;

import java.util.List;

public interface AssetRecordService {
    List<AssetRecord> listAll();
    void addRecord(AssetRecord record);
    void updateRecord(AssetRecord record);
    void deleteRecord(Long id);
    IPage<AssetRecordDTO> pageByConditions(Page<AssetRecord> page, Long typeId, String remark,
                                           String startDate, String endDate, String createUser);
}
