package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionContext {

    private final VerticalStrategy vertical;
    private final HorizontalStrategy horizontal;
    private final ColumnBasedStrategy columnBased;
    private final GeneralStructureStrategy general;

    public List<AssetScanImageDTO> recognizeByScheme(String text, RecognitionScheme scheme) {
        return switch (scheme) {
            case VERTICAL -> vertical.recognize(text);
            case HORIZONTAL -> horizontal.recognize(text);
            case COLUMN_BASED -> columnBased.recognize(text);
            case GENERAL -> general.recognize(text);
        };
    }
}