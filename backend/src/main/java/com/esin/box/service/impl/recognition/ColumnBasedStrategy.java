package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ColumnBasedStrategy implements RecognitionStrategy {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*\\.\\d{2}|\\d{4,}(?:\\.\\d{2})?)");

    // OCR错误纠正
    private static final Map<String, String> OCR_CORRECTIONS = Map.of(
            "讲添益", "鑫添益",
            "蠢尊利", "鑫尊利",
            "恒窒", "恒睿",
            "春添益", "鑫添益",
            "寺盈", "持盈",
            "马", "",
            "吕D", ""
    );

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("分栏结构方案开始识别");

        String[] lines = text.split("\\n");

        // 分析文本结构，找到名称和金额的分界点
        ColumnStructure structure = analyzeColumnStructure(lines);

        if (structure == null) {
            log.debug("未识别到分栏结构");
            return new ArrayList<>();
        }

        // 提取所有产品名称
        List<String> productNames = extractProductNames(lines, structure);

        // 提取所有金额
        List<BigDecimal> amounts = extractAmounts(lines, structure);

        // 配对产品名称和金额
        List<AssetScanImageDTO> results = pairNamesAndAmounts(productNames, amounts);

        log.debug("分栏结构识别完成，结果数量: {}", results.size());
        return results;
    }

    /**
     * 分栏结构信息
     */
    private static class ColumnStructure {
        int nameHeaderIndex = -1;    // "名称"所在行
        int amountHeaderIndex = -1;  // "金额"所在行
        int nameStartIndex = -1;     // 名称数据开始行
        int nameEndIndex = -1;       // 名称数据结束行
        int amountStartIndex = -1;   // 金额数据开始行
        int amountEndIndex = -1;     // 金额数据结束行

        boolean isValid() {
            return nameHeaderIndex >= 0 && amountHeaderIndex >= 0 &&
                    nameStartIndex >= 0 && amountStartIndex >= 0;
        }
    }

    /**
     * 分析分栏结构
     */
    private ColumnStructure analyzeColumnStructure(String[] lines) {
        ColumnStructure structure = new ColumnStructure();

        // 寻找"名称"和"金额"标识行
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.equals("名称")) {
                structure.nameHeaderIndex = i;
                log.debug("找到名称标识行: {}", i);
            } else if (line.equals("金额")) {
                structure.amountHeaderIndex = i;
                log.debug("找到金额标识行: {}", i);
                break; // 找到金额行后就可以停止了
            }
        }

        if (structure.nameHeaderIndex == -1 || structure.amountHeaderIndex == -1) {
            return null;
        }

        // 确定名称数据范围
        structure.nameStartIndex = structure.nameHeaderIndex + 1;
        structure.nameEndIndex = structure.amountHeaderIndex - 1;

        // 确定金额数据范围
        structure.amountStartIndex = structure.amountHeaderIndex + 1;

        // 寻找金额数据的结束位置 - 修复这里的逻辑
        boolean foundAmountEnd = false;
        for (int i = structure.amountStartIndex; i < lines.length; i++) {
            String line = lines[i].trim();

            // 如果是空行，继续寻找
            if (line.isEmpty()) {
                continue;
            }

            // 如果遇到结束标志
            if (line.contains("温馨提示") ||
                    line.contains("收起") ||
                    line.matches("^\\d+、.*") ||  // 1、开头的说明
                    line.contains("您持有的理财产品")) {

                // 向上找到最后一个金额行
                for (int j = i - 1; j >= structure.amountStartIndex; j--) {
                    String prevLine = lines[j].trim();
                    if (isAmountLine(prevLine)) {
                        structure.amountEndIndex = j;
                        foundAmountEnd = true;
                        break;
                    }
                }
                break;
            }

            // 如果是金额行，更新结束位置
            if (isAmountLine(line)) {
                structure.amountEndIndex = i;
            }
        }

        // 如果没有找到明确的结束位置，扫描到最后一个金额行
        if (!foundAmountEnd && structure.amountEndIndex == -1) {
            for (int i = lines.length - 1; i >= structure.amountStartIndex; i--) {
                String line = lines[i].trim();
                if (isAmountLine(line)) {
                    structure.amountEndIndex = i;
                    break;
                }
            }
        }

        log.debug("分栏结构: 名称[{}-{}], 金额[{}-{}]",
                structure.nameStartIndex, structure.nameEndIndex,
                structure.amountStartIndex, structure.amountEndIndex);

        return structure.isValid() ? structure : null;
    }

    /**
     * 判断是否是金额行
     */
    private boolean isAmountLine(String line) {
        if (line.isEmpty()) return false;
        // 检查是否包含金额格式的数字
        return AMOUNT_PATTERN.matcher(line).find();
    }

    /**
     * 提取所有产品名称
     */
    private List<String> extractProductNames(String[] lines, ColumnStructure structure) {
        List<String> productNames = new ArrayList<>();
        List<List<String>> productGroups = new ArrayList<>();
        List<String> currentGroup = new ArrayList<>();

        // 首先按空行分组
        for (int i = structure.nameStartIndex; i <= structure.nameEndIndex; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                if (!currentGroup.isEmpty()) {
                    productGroups.add(new ArrayList<>(currentGroup));
                    currentGroup.clear();
                }
            } else if (!shouldSkipNameLine(line)) {
                currentGroup.add(line);
            }
        }

        // 处理最后一组
        if (!currentGroup.isEmpty()) {
            productGroups.add(currentGroup);
        }

        log.debug("识别到 {} 个产品组", productGroups.size());

        // 将每组合并为产品名称
        for (int i = 0; i < productGroups.size(); i++) {
            List<String> group = productGroups.get(i);
            if (group.isEmpty()) continue;

            String productName = String.join("", group);
            productName = cleanProductName(productName);

            if (isValidProductName(productName)) {
                productNames.add(productName);
                log.debug("产品组{}: {} -> {}", i + 1, group, productName);
            }
        }

        return productNames;
    }

    /**
     * 判断是否应该跳过的名称行
     */
    private boolean shouldSkipNameLine(String line) {
        // 特定的噪音词
        String[] skipWords = {"马", "吕D", "DD", "aa"};
        for (String skip : skipWords) {
            if (line.equals(skip)) {
                return true;
            }
        }

        return line.length() <= 1 ||
                line.matches("^[《》<>()]+$") ||
                line.contains("收起") ||
                line.contains("温馨提示");
    }

    /**
     * 验证产品名称是否有效
     */
    private boolean isValidProductName(String name) {
        // 产品名称应该包含关键词，且长度合理
        return name.length() >= 10 &&
                RecognitionHelper.containsProductKeyword(name);
    }

    /**
     * 提取所有金额
     */
    private List<BigDecimal> extractAmounts(String[] lines, ColumnStructure structure) {
        List<BigDecimal> amounts = new ArrayList<>();

        log.debug("开始提取金额，范围: [{}-{}]", structure.amountStartIndex, structure.amountEndIndex);

        for (int i = structure.amountStartIndex; i <= structure.amountEndIndex; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) continue;

            log.debug("检查第{}行: [{}]", i, line);

            // 提取行中的金额
            Matcher matcher = AMOUNT_PATTERN.matcher(line);
            while (matcher.find()) {
                String amountStr = matcher.group(1);
                BigDecimal amount = parseAmount(amountStr);
                if (amount != null && isValidAmount(amount)) {
                    amounts.add(amount);
                    log.debug("提取金额: {} (来自行: {})", amount, line);
                    break; // 每行只取第一个有效金额
                }
            }
        }

        log.debug("共提取到 {} 个金额", amounts.size());
        return amounts;
    }

    /**
     * 配对产品名称和金额
     */
    private List<AssetScanImageDTO> pairNamesAndAmounts(List<String> names, List<BigDecimal> amounts) {
        List<AssetScanImageDTO> results = new ArrayList<>();

        int pairCount = Math.min(names.size(), amounts.size());
        log.debug("配对产品和金额: {} 个名称, {} 个金额, 将配对 {} 个", names.size(), amounts.size(), pairCount);

        for (int i = 0; i < pairCount; i++) {
            AssetScanImageDTO product = RecognitionHelper.createAssetDTO(names.get(i), amounts.get(i));
            results.add(product);
            log.debug("配对成功: {} -> {}", names.get(i), amounts.get(i));
        }

        // 如果数量不匹配，记录警告
        if (names.size() != amounts.size()) {
            log.warn("产品名称和金额数量不匹配: {} 个名称 vs {} 个金额", names.size(), amounts.size());
        }

        return results;
    }

    /**
     * 清理产品名称
     */
    private String cleanProductName(String name) {
        if (name == null || name.trim().isEmpty()) return "";

        String cleaned = name.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[':-]", "·")
                .replaceAll("[《》\"'（）()\\[\\]{}]", "");

        // 应用OCR纠正
        for (Map.Entry<String, String> entry : OCR_CORRECTIONS.entrySet()) {
            cleaned = cleaned.replace(entry.getKey(), entry.getValue());
        }

        return cleaned;
    }

    /**
     * 解析金额
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            // 处理OCR可能的错误，如 81.,909.31
            String normalized = amountStr.replace(",", "");
            if (normalized.contains(".,")) {
                normalized = normalized.replace(".,", ".");
            }
            return new BigDecimal(normalized);
        } catch (Exception e) {
            log.debug("金额解析失败: {}", amountStr);
            return null;
        }
    }

    /**
     * 验证金额是否有效
     */
    private boolean isValidAmount(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("100")) >= 0 &&
                amount.compareTo(new BigDecimal("500000")) <= 0;
    }
}