package com.esin.box.service;

import com.esin.box.dto.AssetDetailDTO;
import java.util.List;

public interface AssetService {
    List<AssetDetailDTO> getDetailedStatistics(Long assetTypeId, Long assetNameId);
}
