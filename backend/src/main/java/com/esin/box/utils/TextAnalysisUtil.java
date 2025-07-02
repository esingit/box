package com.esin.box.utils;

import com.esin.box.dto.ocr.OcrTextResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文本分析工具类
 * 提供OCR文本分析的公共方法
 */
@Component
public class TextAnalysisUtil {

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
                .toList();

        if (confidences.isEmpty()) {
            return null;
        }

        return confidences.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
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

    /**
     * 合并文本片段 - 修复版
     */
    public OcrTextResult mergeTextFragments(List<OcrTextResult> fragments) {
        if (fragments.size() == 1) {
            return fragments.get(0);
        }

        // 按垂直位置排序
        fragments.sort(Comparator.comparingDouble(t -> getCenterY(t.getBbox())));

        OcrTextResult merged = new OcrTextResult();

        // 合并文本内容
        String combinedText = fragments.stream()
                .map(OcrTextResult::getText)
                .collect(Collectors.joining(""));
        merged.setText(combinedText);

        // 计算平均置信度
        double avgConfidence = fragments.stream()
                .mapToDouble(OcrTextResult::getConfidence)
                .average()
                .orElse(0.0);
        merged.setConfidence(avgConfidence);

        // 计算合并后的边界框 - 修复版
        OcrTextResult.BoundingBox mergedBbox = new OcrTextResult.BoundingBox();
        double left = fragments.stream().mapToDouble(t -> t.getBbox().getLeft()).min().orElse(0);
        double top = fragments.stream().mapToDouble(t -> t.getBbox().getTop()).min().orElse(0);
        double right = fragments.stream().mapToDouble(t -> t.getBbox().getRight()).max().orElse(0);
        double bottom = fragments.stream().mapToDouble(t -> t.getBbox().getBottom()).max().orElse(0);

        mergedBbox.setLeft(left);
        mergedBbox.setTop(top);
        mergedBbox.setRight(right);
        mergedBbox.setBottom(bottom);

        // 重要：设置centerX和centerY
        mergedBbox.setCenterX((left + right) / 2.0);
        mergedBbox.setCenterY((top + bottom) / 2.0);

        merged.setBbox(mergedBbox);

        return merged;
    }

    /**
     * 安全获取centerY，如果为null则计算
     */
    public double getCenterY(OcrTextResult.BoundingBox bbox) {
        if (bbox.getCenterY() != null) {
            return bbox.getCenterY();
        }
        return (bbox.getTop() + bbox.getBottom()) / 2.0;
    }
}