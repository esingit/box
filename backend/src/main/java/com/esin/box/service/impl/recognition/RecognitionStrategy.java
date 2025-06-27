package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;

import java.util.List;

public interface RecognitionStrategy {
    List<AssetScanImageDTO> recognize(String text);
}
