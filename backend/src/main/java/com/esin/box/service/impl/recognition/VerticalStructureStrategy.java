package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class VerticalStructureStrategy implements RecognitionStrategy {

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        List<String> buffer = new ArrayList<>();

        for (String raw : lines) {
            raw = raw.trim();
            if (raw.isEmpty() || RecognitionHelper.shouldSkipLine(raw)) continue;

            if (RecognitionHelper.isAmountLine(raw)) {
                BigDecimal amount = RecognitionHelper.extractAmountFromLine(raw);
                if (!buffer.isEmpty() && amount != null) {
                    String name = RecognitionHelper.cleanName(String.join("", buffer));
                    list.add(RecognitionHelper.createAssetDTO(name, amount));
                    buffer.clear();
                }
            } else {
                buffer.add(raw);
            }
        }
        return list;
    }
}

