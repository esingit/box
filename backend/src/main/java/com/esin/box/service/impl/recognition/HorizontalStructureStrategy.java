package com.esin.box.service.impl.recognition;


import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HorizontalStructureStrategy implements RecognitionStrategy {

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        StringBuilder nameBuf = null;

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty() || RecognitionHelper.shouldSkipLine(raw)) continue;

            if (RecognitionHelper.isNameLine(raw)) {
                String part = RecognitionHelper.cleanName(raw);
                if (nameBuf == null) {
                    nameBuf = new StringBuilder(part);
                } else if (!raw.matches(".*\\d.*") && raw.length() < nameBuf.length()) {
                    nameBuf.append(part);
                } else {
                    nameBuf = new StringBuilder(part);
                }
                continue;
            }

            if (nameBuf != null) {
                BigDecimal amount = RecognitionHelper.findBestAmountInWindow(lines, i, 3);
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(RecognitionHelper.createAssetDTO(nameBuf.toString(), amount));
                    nameBuf = null;
                }
            }
        }
        return list;
    }
}
