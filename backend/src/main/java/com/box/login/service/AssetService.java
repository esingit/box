package com.box.login.service;

import com.box.login.dto.AssetDetailDTO;
import java.util.List;

public interface AssetService {
    List<AssetDetailDTO> getDetailedStatistics(Long assetTypeId, Long assetNameId);
}
