package com.esin.box.utils;

import com.esin.box.dto.ocr.LayoutConfig;
import com.esin.box.dto.ocr.OcrTextResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文本分析工具类
 * 提供OCR文本分析的公共方法
 */
@Component
public class TextAnalysisUtil {

    // 通用金额识别模式
    private static final List<Pattern> AMOUNT_PATTERNS = Arrays.asList(
            Pattern.compile("^[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?$"),
            Pattern.compile("^[0-9]+\\.[0-9]{1,2}$"),
            Pattern.compile("^[0-9]{3,}$")
    );

    // 预编译的正则表达式
    private static final Pattern LETTERS_PATTERN = Pattern.compile(".*[\\p{L}].*");
    private static final Pattern DIGITS_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern PUNCT_PATTERN = Pattern.compile(".*[\\p{Punct}].*");
    private static final Pattern PURE_NUMERIC_PATTERN = Pattern.compile("^[0-9\\s\\p{Punct}]+$");
    private static final Pattern CJK_PATTERN = Pattern.compile(".*[\\u4e00-\\u9fff].*");

    /**
     * 判断是否为有意义的内容
     */
    public boolean isMeaningfulContent(String text) {
        if (text.length() < 2 || text.length() > 50) {
            return false;
        }

        if (PURE_NUMERIC_PATTERN.matcher(text).matches()) {
            return false;
        }

        boolean hasLettersOrChinese = LETTERS_PATTERN.matcher(text).matches() ||
                CJK_PATTERN.matcher(text).matches();

        boolean hasComplexity = hasTextComplexity(text);

        return hasLettersOrChinese && hasComplexity;
    }

    /**
     * 检查文本复杂度
     */
    private boolean hasTextComplexity(String text) {
        int charTypes = 0;

        if (LETTERS_PATTERN.matcher(text).matches()) charTypes++;
        if (CJK_PATTERN.matcher(text).matches()) charTypes++;
        if (DIGITS_PATTERN.matcher(text).matches()) charTypes++;
        if (PUNCT_PATTERN.matcher(text).matches()) charTypes++;

        return charTypes >= 2;
    }

    /**
     * 验证金额有效性
     */
    private boolean isValidAmount(BigDecimal amount) {
        return amount != null &&
                amount.compareTo(LayoutConfig.MIN_AMOUNT) >= 0 &&
                amount.compareTo(LayoutConfig.MAX_AMOUNT) < 0;
    }

    /**
     * 清理产品名称
     */
    public String cleanProductName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }

        return name.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[\\[\\]()（）【】{}]+", "")
                .replaceAll("[,.，。、；;:：]+$", "")
                .trim();
    }

    /**
     * 计算平均置信度
     */
    public Double calculateAverageConfidence(List<OcrTextResult> texts) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }

        List<Double> confidences = texts.stream()
                .map(OcrTextResult::getConfidence)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (confidences.isEmpty()) {
            return null;
        }

        return confidences.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 判断是否为简单标签
     */
    public boolean isSimpleLabel(String text) {
        if (text.length() <= 2) {
            return true;
        }

        if (text.matches(".*[()（）\\[\\]【】].*")) {
            return text.length() <= 8;
        }

        if (text.matches("(.)\\1{2,}")) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为有效文本
     */
    public boolean isValidText(OcrTextResult text) {
        if (StringUtils.isBlank(text.getText()) || text.getBbox() == null) {
            return false;
        }

        String cleanText = text.getText().trim();

        boolean lengthOk = cleanText.length() >= LayoutConfig.MIN_TEXT_LENGTH &&
                cleanText.length() <= LayoutConfig.MAX_TEXT_LENGTH;

        boolean confidenceOk = text.getConfidence() == null ||
                text.getConfidence() >= LayoutConfig.MIN_CONFIDENCE;

        boolean notSimpleLabel = !isSimpleLabel(cleanText);

        return lengthOk && confidenceOk && notSimpleLabel;
    }

    // 在TextAnalysisUtil类中添加或修改以下方法

    /**
     * 解析金额文本为BigDecimal，保留原始精度
     */
    public BigDecimal parseAmount(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        try {
            // 移除所有非数字、小数点、负号的字符
            String cleanText = text.replaceAll("[^\\d.-]", "");

            if (StringUtils.isBlank(cleanText)) {
                return null;
            }

            // 使用BigDecimal保持原始精度
            BigDecimal amount = new BigDecimal(cleanText);

            // 检查是否为有效的正数金额
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            return amount;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查是否为金额格式
     */
    public boolean isAmountFormat(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        // 移除千分位分隔符和其他非数字字符，保留数字、小数点、负号
        String cleanText = text.replaceAll("[^\\d.-]", "");

        if (StringUtils.isBlank(cleanText)) {
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(cleanText);
            return amount.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 格式化金额显示 - 保留原始精度，确保至少两位小数
     */
    public String formatAmountDisplay(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        // 保留原始小数位数，但至少显示两位
        int scale = Math.max(amount.scale(), 2);

        // 使用DecimalFormat来格式化，保持原始精度
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(2); // 最少两位小数
        df.setMaximumFractionDigits(scale); // 最多保留原始精度
        df.setGroupingUsed(false); // 不使用千分位分隔符

        return df.format(amount);
    }
}