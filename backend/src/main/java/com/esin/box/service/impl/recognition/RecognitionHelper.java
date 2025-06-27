package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            } catch (Exception ignored) {
            }
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
            } catch (Exception ignored) {
            }
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

    // 判断是否为表头行
    private static boolean isHeaderLine(String line) {
        if (line == null || line.trim().isEmpty()) return false;

        String normalized = line.replaceAll("\\s+", "").toLowerCase();

        // 检查是否包含名称相关的词
        boolean hasNameKeyword = normalized.contains("名称") ||
                normalized.contains("产品名") ||
                normalized.contains("产品") ||
                normalized.contains("名字");

        // 检查是否包含金额相关的词
        boolean hasAmountKeyword = normalized.contains("金额") ||
                normalized.contains("持仓") ||
                normalized.contains("市值") ||
                normalized.contains("余额") ||
                normalized.contains("资产");

        return hasNameKeyword && hasAmountKeyword;
    }

    public static List<AssetScanImageDTO> parseWithIntelligentMatching(String[] lines) {
        List<AssetScanImageDTO> list = new ArrayList<>();
        int startIndex = 0;
        boolean foundHeader = false;

        // 1. 寻找表头行（支持多种表头格式，且只在前30行查找）
        int headerSearchLimit = Math.min(lines.length, 30);
        for (int i = 0; i < headerSearchLimit; i++) {
            String line = lines[i].trim();
            if (isHeaderLine(line)) {
                startIndex = i + 1; // 从表头下一行开始
                foundHeader = true;
                System.out.println("找到表头在第 " + i + " 行: " + line);
                break;
            }
        }

        // 如果没找到表头，从第一行开始
        if (!foundHeader) {
            System.out.println("未找到表头，从第一行开始解析");
            startIndex = 0;
        }

        // 2. 存储已处理的行索引，避免重复处理
        Set<Integer> processedLines = new HashSet<>();

        // 3. 正式开始识别
        for (int i = startIndex; i < lines.length; i++) {
            // 检查是否遇到新的表头（处理多表格情况）
            if (i > startIndex && isHeaderLine(lines[i].trim())) {
                System.out.println("发现新表头在第 " + i + " 行");
                continue; // 跳过表头行本身
            }

            String line = lines[i].trim();
            if (line.isEmpty() || shouldSkipLine(line) || processedLines.contains(i)) continue;

            // 当前行是纯金额行
            if (isAmountLine(line)) {
                BigDecimal amount = extractAmountFromLine(line);
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 回溯上面最多 3 行拼接名称
                StringBuilder nameBuilder = new StringBuilder();
                List<Integer> usedLines = new ArrayList<>();

                for (int j = Math.max(startIndex, i - 3); j < i; j++) {
                    if (processedLines.contains(j)) continue;

                    String candidate = lines[j].trim();
                    if (!shouldSkipLine(candidate) && containsChinese(candidate) && !isAmountLine(candidate)) {
                        if (nameBuilder.length() > 0) nameBuilder.append("·");
                        nameBuilder.append(candidate);
                        usedLines.add(j);
                    }
                }

                String rawName = nameBuilder.toString();
                if (!rawName.isEmpty() && containsProductKeyword(rawName)) {
                    String name = cleanName(rawName);
                    list.add(createAssetDTO(name, amount));

                    // 标记已使用的行
                    processedLines.add(i);
                    processedLines.addAll(usedLines);
                }
            }
            // 当前行包含名称和金额
            else if (containsChinese(line) && containsAmount(line)) {
                // 尝试从同一行提取名称和金额
                String possibleName = extractNameFromMixedLine(line);
                BigDecimal amount = extractAmountFromLine(line);

                if (possibleName != null && !possibleName.isEmpty() &&
                        amount != null && amount.compareTo(BigDecimal.ZERO) > 0 &&
                        containsProductKeyword(possibleName)) {

                    String name = cleanName(possibleName);
                    list.add(createAssetDTO(name, amount));
                    processedLines.add(i);
                }
            }
        }

        return list;
    }

    // 从混合行中提取产品名称（去除金额部分）
    private static String extractNameFromMixedLine(String line) {
        // 移除所有数字、逗号、点号组成的部分
        String nameOnly = line.replaceAll("[\\d.,]+", " ").trim();
        // 移除多余空格
        nameOnly = nameOnly.replaceAll("\\s+", " ");
        return nameOnly.trim();
    }

    // 判断是否包含中文
    public static boolean containsChinese(String text) {
        return text != null && text.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    // 清洗产品名
    public static String cleanName(String name) {
        return name.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[':-]", "·")
                .replaceAll("[《》\"''()\\[\\]{}]", "")
                .replaceAll("·+", "·"); // 避免多个连续的·
    }
}