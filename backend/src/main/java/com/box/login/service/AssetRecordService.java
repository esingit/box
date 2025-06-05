package com.box.login.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;

public interface AssetRecordService extends IService<AssetRecord> {
    AssetRecordDTO getDetailById(Long id);
    Page<AssetRecordDTO> pageWithDetails(Page<AssetRecord> page, String assetName);
}
