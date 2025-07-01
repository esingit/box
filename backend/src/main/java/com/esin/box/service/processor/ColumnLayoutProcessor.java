package com.esin.box.service.processor;

import com.esin.box.dto.ocr.*;
import com.esin.box.utils.TextAnalysisUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 左右列布局处理器（金额驱动优化版）
 * 以金额为核心，智能识别相关文本并组合成完整产品
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColumnLayoutProcessor implements LayoutProcessor {

    private final TextAnalysisUtil textAnalysisUtil;

    // 常量定义
    private static final double SAME_ROW_TOLERANCE = 50.0; // 同行容差
    private static final double NEARBY_ROW_TOLERANCE = 80.0; // 邻近行容差
    private static final int MIN_CONTENT_LENGTH = 2; // 最小内容长度

    @Override
    public List<ProductItem> processLayout(PageLayout layout) {
        log.info("=== 开始金额驱动的布局处理 ===");
        log.info("有效文本数量: {}", layout.getValidTexts().size());
        log.info("页面尺寸: {}x{}", layout.getWidth(), layout.getHeight());

        try {
            if (layout.getValidTexts().isEmpty()) {
                log.warn("没有有效文本数据");
                return Collections.emptyList();
            }

            // 1. 识别和过滤文本
            ProcessedTexts processedTexts = processAndClassifyTexts(layout.getValidTexts());
            log.info("文本处理完成: 金额文本={}, 内容文本={}, 无关文本={}",
                    processedTexts.getAmountTexts().size(),
                    processedTexts.getContentTexts().size(),
                    processedTexts.getIrrelevantTexts().size());

            if (processedTexts.getAmountTexts().isEmpty()) {
                log.warn("没有识别到有效金额");
                return Collections.emptyList();
            }

            // 2. 分析列结构
            ColumnBoundary columnBoundary = analyzeColumnBoundary(processedTexts, layout.getWidth());
            log.info("列边界分析完成: 边界位置={}", columnBoundary.getBoundary());

            // 3. 基于金额构建产品
            List<ProductItem> products = buildProductsFromAmounts(processedTexts, columnBoundary);

            // 4. 排序和输出
            products.sort(Comparator.comparingDouble(ProductItem::getPagePosition));

            log.info("金额驱动处理完成，产品数量: {}", products.size());
            logProductDetails(products);

            return products;

        } catch (Exception e) {
            log.error("金额驱动处理异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 处理和分类文本
     */
    private ProcessedTexts processAndClassifyTexts(List<OcrTextResult> texts) {
        ProcessedTexts result = new ProcessedTexts();

        for (OcrTextResult text : texts) {
            String content = text.getText().trim();

            if (StringUtils.isBlank(content)) {
                continue;
            }

            // 识别金额
            if (textAnalysisUtil.isAmountFormat(content)) {
                BigDecimal amount = textAnalysisUtil.parseAmount(content);
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    result.getAmountTexts().add(text);
                    log.debug("识别金额: '{}' -> {}", content, amount);
                    continue;
                }
            }

            // 过滤无关文本
            if (isIrrelevantText(content)) {
                result.getIrrelevantTexts().add(text);
                log.debug("过滤无关文本: '{}'", content);
                continue;
            }

            // 其余作为内容文本
            if (content.length() >= MIN_CONTENT_LENGTH) {
                result.getContentTexts().add(text);
                log.debug("识别内容文本: '{}'", content);
            }
        }

        return result;
    }

    /**
     * 判断是否为无关文本
     */
    private boolean isIrrelevantText(String text) {
        String[] irrelevantPatterns = {
                "理财", "总金额", "名称", "金额", "收起", "温馨提示", "（元）"
        };

        for (String pattern : irrelevantPatterns) {
            if (text.equals(pattern) || text.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 分析列边界
     */
    private ColumnBoundary analyzeColumnBoundary(ProcessedTexts texts, double pageWidth) {
        ColumnBoundary boundary = new ColumnBoundary();

        if (!texts.getAmountTexts().isEmpty()) {
            // 基于金额位置确定右列
            double avgAmountX = texts.getAmountTexts().stream()
                    .mapToDouble(t -> t.getBbox().getLeft())
                    .average()
                    .orElse(pageWidth * 0.7);

            // 基于内容位置确定左列
            double avgContentX = texts.getContentTexts().stream()
                    .mapToDouble(t -> t.getBbox().getLeft())
                    .average()
                    .orElse(pageWidth * 0.1);

            boundary.setBoundary((avgAmountX + avgContentX) / 2);
        } else {
            boundary.setBoundary(pageWidth / 2);
        }

        log.debug("列边界计算: 金额平均X={}, 内容平均X={}, 边界={}",
                texts.getAmountTexts().stream().mapToDouble(t -> t.getBbox().getLeft()).average().orElse(0),
                texts.getContentTexts().stream().mapToDouble(t -> t.getBbox().getLeft()).average().orElse(0),
                boundary.getBoundary());

        return boundary;
    }

    /**
     * 基于金额构建产品
     */
    private List<ProductItem> buildProductsFromAmounts(ProcessedTexts texts, ColumnBoundary columnBoundary) {
        List<ProductItem> products = new ArrayList<>();

        // 按Y坐标排序金额
        List<OcrTextResult> sortedAmounts = texts.getAmountTexts().stream()
                .sorted(Comparator.comparingDouble(t -> t.getBbox().getCenterY()))
                .toList();

        for (int i = 0; i < sortedAmounts.size(); i++) {
            OcrTextResult amountText = sortedAmounts.get(i);
            BigDecimal amount = textAnalysisUtil.parseAmount(amountText.getText().trim());

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            log.debug("处理金额: '{}' (Y={}, 金额={})",
                    amountText.getText(), amountText.getBbox().getCenterY(), amount);

            // 为该金额找到相关的内容文本
            List<OcrTextResult> relatedTexts = findRelatedTexts(amountText, texts.getContentTexts(), columnBoundary);

            if (relatedTexts.isEmpty()) {
                log.warn("金额 '{}' 没有找到相关内容文本", amountText.getText());
                continue;
            }

            // 构建产品项
            ProductItem product = buildProduct(amountText, amount, relatedTexts, i);
            if (product != null) {
                products.add(product);
                log.info("构建产品成功: '{}' - {}",
                        product.getCleanName(), textAnalysisUtil.formatAmountDisplay(amount));
            }
        }

        return products;
    }

    /**
     * 为金额找到相关的内容文本
     */
    private List<OcrTextResult> findRelatedTexts(OcrTextResult amountText,
                                                 List<OcrTextResult> contentTexts,
                                                 ColumnBoundary columnBoundary) {
        double amountY = amountText.getBbox().getCenterY();
        List<OcrTextResult> candidates = new ArrayList<>();

        // 第一阶段：查找同行文本
        for (OcrTextResult contentText : contentTexts) {
            double contentY = contentText.getBbox().getCenterY();
            double distance = Math.abs(amountY - contentY);

            if (distance <= SAME_ROW_TOLERANCE) {
                candidates.add(contentText);
                log.debug("找到同行文本: '{}' (距离={})", contentText.getText(), distance);
            }
        }

        // 第二阶段：如果同行文本不足，查找邻近行文本
        if (candidates.size() < 2) {
            for (OcrTextResult contentText : contentTexts) {
                if (candidates.contains(contentText)) {
                    continue;
                }

                double contentY = contentText.getBbox().getCenterY();
                double distance = Math.abs(amountY - contentY);

                if (distance <= NEARBY_ROW_TOLERANCE) {
                    // 检查是否在合理的位置范围内
                    if (isInReasonablePosition(contentText, amountText, columnBoundary)) {
                        candidates.add(contentText);
                        log.debug("找到邻近文本: '{}' (距离={})", contentText.getText(), distance);
                    }
                }
            }
        }

        // 按位置排序
        candidates.sort((a, b) -> {
            double yA = a.getBbox().getCenterY();
            double yB = b.getBbox().getCenterY();
            if (Math.abs(yA - yB) <= 10.0) {
                return Double.compare(a.getBbox().getLeft(), b.getBbox().getLeft());
            }
            return Double.compare(yA, yB);
        });

        return candidates;
    }

    /**
     * 检查文本是否在合理位置
     */
    private boolean isInReasonablePosition(OcrTextResult contentText, OcrTextResult amountText, ColumnBoundary columnBoundary) {
        double contentX = contentText.getBbox().getLeft();
        double amountX = amountText.getBbox().getLeft();

        // 内容文本应该在左侧，金额在右侧
        boolean contentOnLeft = contentX < columnBoundary.getBoundary();
        boolean amountOnRight = amountX >= columnBoundary.getBoundary();

        if (contentOnLeft && amountOnRight) {
            return true;
        }

        // 如果不符合标准布局，检查是否距离很近（可能是特殊布局）
        return Math.abs(contentX - amountX) <= 100.0;
    }

    /**
     * 构建单个产品项
     */
    private ProductItem buildProduct(OcrTextResult amountText, BigDecimal amount,
                                     List<OcrTextResult> relatedTexts, int index) {
        if (relatedTexts.isEmpty()) {
            return null;
        }

        ProductItem product = new ProductItem();

        // 设置金额
        if (amount.scale() < 2) {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        }
        product.setAmountText(amountText);
        product.setAmount(amount);

        // 构建产品名称
        String originalName = relatedTexts.stream()
                .map(OcrTextResult::getText)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));

        if (StringUtils.isBlank(originalName)) {
            log.warn("产品名称为空，跳过");
            return null;
        }

        product.setOriginalName(originalName);
        product.setCleanName(textAnalysisUtil.cleanProductName(originalName));
        product.setRelatedTexts(relatedTexts);

        // 设置位置信息
        List<OcrTextResult> allTexts = new ArrayList<>(relatedTexts);
        allTexts.add(amountText);

        double avgY = allTexts.stream()
                .mapToDouble(t -> t.getBbox().getCenterY())
                .average()
                .orElse(0);

        product.setPagePosition(avgY);
        product.setRowIndex(index);

        // 计算置信度
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));

        log.debug("构建产品: 原始='{}', 清理='{}', 金额={}, 相关文本数={}",
                originalName, product.getCleanName(),
                textAnalysisUtil.formatAmountDisplay(amount), relatedTexts.size());

        return product;
    }

    /**
     * 输出产品详情
     */
    private void logProductDetails(List<ProductItem> products) {
        if (products.isEmpty()) {
            log.warn("没有成功构建任何产品");
            return;
        }

        log.info("产品按页面顺序排列：");
        for (int i = 0; i < products.size(); i++) {
            ProductItem p = products.get(i);
            String formattedAmount = textAnalysisUtil.formatAmountDisplay(p.getAmount());
            log.info("  {}. '{}' - {} (Y位置: {})",
                    i + 1, p.getCleanName(), formattedAmount, p.getPagePosition());
        }
    }

    /**
     * 处理后的文本分类
     */
    @Getter
    private static class ProcessedTexts {
        private final List<OcrTextResult> amountTexts = new ArrayList<>();
        private final List<OcrTextResult> contentTexts = new ArrayList<>();
        private final List<OcrTextResult> irrelevantTexts = new ArrayList<>();

    }

    /**
     * 列边界信息
     */
    @Setter
    @Getter
    private static class ColumnBoundary {
        private double boundary;
    }
}