package com.esin.box.service.impl;

import com.esin.box.dto.AssetDetailDTO;
import com.esin.box.mapper.AssetRecordMapper;
import com.esin.box.service.AssetService;
import com.esin.box.utils.SecurityUtils;
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
