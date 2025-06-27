package com.esin.box.service.impl.recognition;


import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HorizontalStructureStrategy implements RecognitionStrategy {

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        StringBuilder nameBuf = null;

        Pattern pattern = Pattern.compile("(.+?)\\s+([\\d,]+\\.?\\d*)");

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty() || RecognitionHelper.shouldSkipLine(raw)) continue;

            Matcher matcher = pattern.matcher(raw);
            if (matcher.matches()) {
                // 左右结构一行搞定
                String name = RecognitionHelper.cleanName(matcher.group(1));
                String amountStr = matcher.group(2);
                BigDecimal amount = RecognitionHelper.parseAmount(RecognitionHelper.normalizeAmountStr(amountStr));
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(RecognitionHelper.createAssetDTO(name, amount));
                    continue;
                }
            }

            // 尝试备用策略：产品名一行，金额在下一行
            if (RecognitionHelper.isNameLine(raw)) {
                String part = RecognitionHelper.cleanName(raw);
                if (nameBuf == null) {
                    nameBuf = new StringBuilder(part);
                } else {
                    nameBuf.append(part);
                }
                continue;
            }

            if (nameBuf != null) {
                BigDecimal amount = RecognitionHelper.findBestAmountInWindow(lines, i, 2);
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(RecognitionHelper.createAssetDTO(nameBuf.toString(), amount));
                    nameBuf = null;
                }
            }
        }
        return list;
    }
}
