package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecognitionHelper {

    public static final Set<String> PRODUCT_KEYWORDS = Set.of("理财", "基金", "债券", "存款", "产品", "固收", "开放", "净值",
            "添益", "尊利", "持盈", "鑫", "恒", "创利", "精选", "成长", "灵活", "稳健", "增利", "宝", "通", "汇", "融", "投");

    public static boolean containsProductKeyword(String line) {
        return PRODUCT_KEYWORDS.stream().anyMatch(line::contains);
    }

    public static boolean containsAmount(String line) {
        Matcher matcher = Pattern.compile("[\\d.,]+").matcher(line);
        while (matcher.find()) {
            String amount = matcher.group();
            if (amount.length() >= 4 && !amount.contains(".") && !amount.contains(",")) continue;
            try {
                BigDecimal value = new BigDecimal(normalizeAmountStr(amount));
                if (value.compareTo(BigDecimal.ZERO) > 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean shouldSkipLine(String line) {
        String[] skip = {
                "总金额", "收起", "温馨提示", "持仓市值", "持仓收益", "可赎回",
                "最短持有期", "每日可赎", "赎回类型"
        };
        for (String p : skip) if (line.startsWith(p)) return true;
        if (line.contains("持仓") && line.contains("撤单")) return true;
        if (line.equals("撤单") || line.equals("撤音")) return true;
        if (line.matches("^\\d{1,2}:\\d{1,2}$")) return true;
        if (line.matches("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*$")) return true;
        return false;
    }

    public static boolean isNameLine(String line) {
        if (!line.matches(".*[\\u4e00-\\u9fa5].*")) return false;
        return containsProductKeyword(line) && !line.contains("撤单") && !line.contains("撤音") && !line.contains("持仓撤单");
    }

    public static boolean isAmountLine(String line) {
        return line.matches("^[\\d,\\.]+$");
    }

    public static String normalizeAmountStr(String s) {
        s = s.replaceAll("\\s+", "").replace(",", "");
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0) {
            String intPart = s.substring(0, lastDot).replace(".", "");
            String fracPart = s.substring(lastDot + 1);
            return intPart + "." + fracPart;
        }
        return s;
    }

    public static BigDecimal parseAmount(String s) {
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal extractAmountFromLine(String line) {
        Matcher matcher = Pattern.compile("[\\d.,]+").matcher(line);
        BigDecimal lastValid = null;
        while (matcher.find()) {
            String raw = matcher.group();
            String normalized = normalizeAmountStr(raw);
            try {
                BigDecimal value = new BigDecimal(normalized);
                lastValid = value;
            } catch (Exception ignored) {}
        }
        return lastValid;
    }

    public static BigDecimal findBestAmountInWindow(String[] lines, int startIndex, int maxLines) {
        BigDecimal best = null;
        for (int j = startIndex; j < Math.min(lines.length, startIndex + maxLines); j++) {
            Matcher m = Pattern.compile("[\\d.,]+").matcher(lines[j]);
            while (m.find()) {
                String norm = normalizeAmountStr(m.group());
                BigDecimal val = parseAmount(norm);
                if (val != null && (best == null || val.compareTo(best) > 0)) {
                    best = val;
                }
            }
        }
        return best;
    }

    public static AssetScanImageDTO createAssetDTO(String name, BigDecimal amount) {
        AssetScanImageDTO dto = new AssetScanImageDTO();
        dto.setOriginalAssetName(name);
        dto.setCleanedAssetName(name);
        dto.setAssetName(name);
        dto.setAmount(amount);
        dto.setAcquireTime(LocalDateTime.now());
        dto.setRemark("图片识别导入");
        return dto;
    }

    public static List<AssetScanImageDTO> parseWithIntelligentMatching(String[] lines) {
        List<AssetScanImageDTO> list = new ArrayList<>();
        List<String> productNames = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || shouldSkipLine(line)) continue;
            if (containsProductKeyword(line) && !containsAmount(line)) {
                productNames.add(cleanName(line));
            }
        }
        for (String productName : productNames) {
            int productIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains(productName)) {
                    productIndex = i;
                    break;
                }
            }
            if (productIndex == -1) continue;
            BigDecimal amount = extractAmountFromLine(lines[productIndex]);
            if (amount == null) {
                for (int i = 1; i <= 3 && productIndex + i < lines.length; i++) {
                    amount = extractAmountFromLine(lines[productIndex + i]);
                    if (amount != null) break;
                }
            }
            if (amount == null) {
                for (int i = 1; i <= 2 && productIndex - i >= 0; i++) {
                    amount = extractAmountFromLine(lines[productIndex - i]);
                    if (amount != null) break;
                }
            }
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                list.add(createAssetDTO(productName, amount));
            }
        }
        return list;
    }

    public static String cleanName(String name) {
        return name.trim().replaceAll("\\s+", "")
                .replaceAll("[':-]", "·")
                .replaceAll("[《》\"''()\\[\\]{}]", "");
    }
}