package com.box.login.service.impl;

import com.box.login.dto.AssetDetailDTO;
import com.box.login.mapper.AssetRecordMapper;
import com.box.login.service.AssetService;
import com.box.login.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;

    @Override
    public List<AssetDetailDTO> getDetailedStatistics(Long assetTypeId, Long assetNameId) {
        String userId = SecurityUtils.getCurrentUserId();
        return assetRecordMapper.getDetailedStatistics(userId, assetTypeId, assetNameId);
    }
}
