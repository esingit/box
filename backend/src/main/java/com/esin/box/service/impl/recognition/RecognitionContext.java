package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionContext {

    private final HorizontalStructureStrategy horizontal;
    private final VerticalStructureStrategy vertical;
    private final GeneralStructureStrategy general;

    public List<AssetScanImageDTO> dispatch(String text, Enum<?> format) {
        if (format.name().equals("HORIZONTAL")) return horizontal.recognize(text);
        if (format.name().equals("VERTICAL")) return vertical.recognize(text);
        return general.recognize(text);
    }
}

