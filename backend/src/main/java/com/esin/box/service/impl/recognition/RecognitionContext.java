package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionContext {

    private final Scheme1Strategy scheme1;
    private final Scheme2Strategy scheme2;
    private final GeneralStructureStrategy general;

    public List<AssetScanImageDTO> recognizeByScheme(String text, RecognitionScheme scheme) {
        switch (scheme) {
            case SCHEME_1:
                return scheme1.recognize(text);
            case SCHEME_2:
                return scheme2.recognize(text);
            case GENERAL:
                return general.recognize(text);
            default:
                return general.recognize(text);
        }
    }
}