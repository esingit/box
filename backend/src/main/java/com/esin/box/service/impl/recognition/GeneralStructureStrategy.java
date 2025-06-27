package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeneralStructureStrategy implements RecognitionStrategy {

    private static final Pattern pattern1 = Pattern.compile("(.+?)([\\d,]+\\.?\\d*)$");
    private static final Pattern pattern2 = Pattern.compile("(.+?)\\s+([\\d,]+\\.?\\d*)");

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || RecognitionHelper.shouldSkipLine(line)) continue;

            Matcher matcher = pattern1.matcher(line);
            if (matcher.find()) {
                String name = RecognitionHelper.cleanName(matcher.group(1));
                String amountStr = matcher.group(2);
                BigDecimal amount = RecognitionHelper.parseAmount(RecognitionHelper.normalizeAmountStr(amountStr));
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(RecognitionHelper.createAssetDTO(name, amount));
                    continue;
                }
            }

            matcher = pattern2.matcher(line);
            if (matcher.find()) {
                String name = RecognitionHelper.cleanName(matcher.group(1));
                String amountStr = matcher.group(2);
                BigDecimal amount = RecognitionHelper.parseAmount(RecognitionHelper.normalizeAmountStr(amountStr));
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(RecognitionHelper.createAssetDTO(name, amount));
                }
            }
        }

        if (list.isEmpty()) {
            list = RecognitionHelper.parseWithIntelligentMatching(lines);
        }
        return list;
    }
}