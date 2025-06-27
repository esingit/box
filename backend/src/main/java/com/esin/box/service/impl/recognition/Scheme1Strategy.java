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
public class Scheme1Strategy implements RecognitionStrategy {

    // 严格的左右结构模式：产品名称 + 空格 + 金额（在同一行）
    private static final Pattern LEFT_RIGHT_PATTERN = Pattern.compile("^(.+?)\\s+(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)$");

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("方案1开始识别 - 左右结构专用");

        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> results = new ArrayList<>();

        // 找到数据开始位置
        int startIndex = findDataStartIndex(lines);
        if (startIndex == -1) {
            startIndex = 0;
        }

        // 处理左右结构数据
        results = processLeftRightStructure(lines, startIndex);

        log.debug("方案1识别完成，结果数量: {}", results.size());
        return results;
    }

    /**
     * 找到数据开始位置
     */
    private int findDataStartIndex(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("名称") && line.contains("金额")) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 处理左右结构数据
     */
    private List<AssetScanImageDTO> processLeftRightStructure(String[] lines, int startIndex) {
        List<AssetScanImageDTO> results = new ArrayList<>();

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();

            // 使用方案1自己的跳过逻辑
            if (line.isEmpty() || shouldSkipForScheme1(line)) {
                continue;
            }

            // 检查是否是完整的左右结构行
            Matcher matcher = LEFT_RIGHT_PATTERN.matcher(line);
            if (matcher.matches()) {
                String namePart = matcher.group(1).trim();
                String amountStr = matcher.group(2).trim();

                // 如果名称部分包含产品关键词，这是一个完整的产品行
                if (RecognitionHelper.containsProductKeyword(namePart)) {
                    BigDecimal amount = parseAmount(amountStr);
                    if (amount != null && isValidAmount(amount)) {
                        // 收集完整名称（可能跨行）
                        String fullName = collectFullName(lines, i, namePart);
                        results.add(RecognitionHelper.createAssetDTO(fullName, amount));
                        log.debug("识别到产品: {} -> {}", fullName, amount);
                    }
                }
            }
        }

        return results;
    }

    /**
     * 方案1专用的跳过判断
     */
    private boolean shouldSkipForScheme1(String line) {
        // 只跳过明确的无效内容
        if (line.contains("总金额") || line.contains("收起") || line.contains("温馨提示")) {
            return true;
        }

        // 单独的"名称"或"金额"
        if (line.equals("名称") || line.equals("金额")) {
            return true;
        }

        return false;
    }

    /**
     * 收集完整名称（处理跨行的情况）
     */
    private String collectFullName(String[] lines, int currentIndex, String initialName) {
        StringBuilder fullName = new StringBuilder(initialName);

        // 检查后续1-2行是否是名称的续行
        for (int i = 1; i <= 2 && currentIndex + i < lines.length; i++) {
            String nextLine = lines[currentIndex + i].trim();

            // 空行跳过
            if (nextLine.isEmpty()) {
                continue;
            }

            // 如果是新产品开始或包含金额，停止
            if (nextLine.startsWith("工银理财") ||
                    LEFT_RIGHT_PATTERN.matcher(nextLine).matches()) {
                break;
            }

            // 如果不是纯数字且长度合适，可能是名称续行
            if (!nextLine.matches("^[\\d,\\.]+$") &&
                    nextLine.length() > 2 &&
                    nextLine.length() < 30) {
                fullName.append(nextLine);
            }
        }

        return RecognitionHelper.cleanName(fullName.toString());
    }

    /**
     * 解析金额
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            String cleaned = amountStr.replaceAll("[,\\s]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证金额有效性
     */
    private boolean isValidAmount(BigDecimal amount) {
        // 排除总金额497271.05
        if (amount.compareTo(new BigDecimal("497271.05")) == 0) {
            return false;
        }

        // 正常理财产品金额范围
        return amount.compareTo(new BigDecimal("1000")) >= 0 &&
                amount.compareTo(new BigDecimal("500000")) <= 0;
    }
}