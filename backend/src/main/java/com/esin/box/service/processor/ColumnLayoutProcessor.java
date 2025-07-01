package com.esin.box.service.processor;

import com.esin.box.dto.ocr.OcrTextResult;
import com.esin.box.dto.ocr.PageLayout;
import com.esin.box.dto.ocr.ProductItem;
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
 * 左右列布局处理器（修复版）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColumnLayoutProcessor implements LayoutProcessor {

    private final TextAnalysisUtil textAnalysisUtil;

    // 修复后的常量定义
    private static final double SAME_ROW_TOLERANCE = 60.0;
    private static final double NEARBY_ROW_TOLERANCE = 120.0;
    private static final double PRODUCT_NAME_MERGE_DISTANCE = 50.0;
    private static final int MIN_CONTENT_LENGTH = 2;
    private static final double MIN_VALID_AMOUNT = 0.01;
    private static final double MAX_VALID_AMOUNT = 100000000.0;

    /**
     * 严格的无关文本判断 - 增强版
     */
    private boolean isIrrelevantTextStrict(String text) {
        Set<String> irrelevantTexts = Set.of(
                "总金额", "总金额（元）", "合计", "小计", "收起", "展开",
                "温馨提示", "（元）", "¥", "$", "名称", "金额", "理财",
                "产品持仓", "撤单", "持仓市值", "持仓币值", "可赎回日",
                "赎回类型", "赎回尖型", "今日可赎", "每日可赎", "预约赎回",
                "周期结束日", "可赎回开始日", "最短持有期内不可赎回，可赎回开始日起每日可赎"
        );

        String trimmed = text.trim();

        // 完全匹配检查
        if (irrelevantTexts.contains(trimmed)) {
            return true;
        }

        // 检查是否包含总金额相关文本
        if (trimmed.contains("总金额") || trimmed.contains("合计") || trimmed.contains("小计")) {
            return true;
        }

        return false;
    }

    /**
     * 修复的产品构建 - 避免将元数据作为产品名称
     */
    private ProductItem buildProductFixed(OcrTextResult amountText, BigDecimal amount,
                                          List<OcrTextResult> relatedTexts, int index) {
        ProductItem product = new ProductItem();

        if (amount.scale() < 2) {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        }
        product.setAmountText(amountText);
        product.setAmount(amount);

        // 构建产品名称 - 过滤掉元数据
        String originalName = buildProductNameFixed(relatedTexts);

        // 如果构建的名称是无效的，使用默认名称
        if (StringUtils.isBlank(originalName) ||
                isIrrelevantTextStrict(originalName) ||
                originalName.length() < 4) {
            originalName = "理财产品 " + (index + 1);
        }

        product.setOriginalName(originalName);
        product.setCleanName(textAnalysisUtil.cleanProductName(originalName));
        product.setRelatedTexts(relatedTexts);

        // 设置位置信息
        List<OcrTextResult> allTexts = new ArrayList<>(relatedTexts);
        allTexts.add(amountText);

        double avgY = allTexts.stream()
                .mapToDouble(t -> getCenterY(t.getBbox()))
                .average()
                .orElse(getCenterY(amountText.getBbox()));

        product.setPagePosition(avgY);
        product.setRowIndex(index);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));

        return product;
    }

    /**
     * 修复的产品名称构建 - 增强过滤逻辑
     */
    private String buildProductNameFixed(List<OcrTextResult> texts) {
        if (texts.isEmpty()) {
            return "";
        }

        // 过滤和排序文本
        List<String> validTexts = texts.stream()
                .map(OcrTextResult::getText)
                .filter(StringUtils::isNotBlank)
                .filter(text -> !isIrrelevantTextStrict(text))
                .filter(text -> text.length() > 1)
                .filter(text -> isProductNameCandidate(text))
                .collect(Collectors.toList());

        if (validTexts.isEmpty()) {
            // 尝试获取第一个非元数据文本
            for (OcrTextResult text : texts) {
                if (!isIrrelevantTextStrict(text.getText()) && text.getText().length() > 3) {
                    return text.getText();
                }
            }
            return "";
        }

        // 如果只有一个有效文本且长度足够，直接使用
        if (validTexts.size() == 1 && validTexts.get(0).length() >= 6) {
            return validTexts.get(0);
        }

        // 智能组合多个文本
        return validTexts.stream()
                .collect(Collectors.joining(""));
    }

    @Override
    public List<ProductItem> processLayout(PageLayout layout) {
        log.info("=== 开始左右列布局处理（修复版） ===");
        log.info("有效文本数量: {}", layout.getValidTexts().size());

        try {
            if (layout.getValidTexts().isEmpty()) {
                log.warn("没有有效文本数据");
                return Collections.emptyList();
            }

            // 1. 预处理：合并被拆分的产品名称
            List<OcrTextResult> mergedTexts = mergeFragmentedProductNames(layout.getValidTexts());
            log.info("文本合并后数量: {}", mergedTexts.size());

            // 2. 识别和分类文本（使用严格的金额判断）
            ProcessedTexts processedTexts = processAndClassifyTextsFixed(mergedTexts);
            log.info("文本分类完成: 金额={}, 内容={}, 无关={}",
                    processedTexts.getAmountTexts().size(),
                    processedTexts.getContentTexts().size(),
                    processedTexts.getIrrelevantTexts().size());

            if (processedTexts.getAmountTexts().isEmpty()) {
                log.warn("没有识别到有效金额");
                return Collections.emptyList();
            }

            // 3. 分析列结构
            ColumnBoundary columnBoundary = analyzeColumnBoundary(processedTexts, layout.getWidth());

            // 4. 基于金额构建产品（修复版）
            List<ProductItem> products = buildProductsFromAmountsFixed(processedTexts, columnBoundary);

            // 5. 排序和验证
            products = validateAndSortProducts(products);

            log.info("左右列处理完成，产品数量: {}", products.size());
            logProductDetails(products);

            return products;

        } catch (Exception e) {
            log.error("左右列布局处理异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 合并被拆分的产品名称
     */
    private List<OcrTextResult> mergeFragmentedProductNames(List<OcrTextResult> texts) {
        log.debug("开始合并被拆分的产品名称");

        List<OcrTextResult> result = new ArrayList<>();
        Set<Integer> mergedIndexes = new HashSet<>();

        for (int i = 0; i < texts.size(); i++) {
            if (mergedIndexes.contains(i)) {
                continue;
            }

            OcrTextResult currentText = texts.get(i);

            // 检查是否是产品名称的一部分
            if (isProductNameFragment(currentText.getText())) {
                // 寻找相邻的文本片段进行合并
                List<OcrTextResult> fragments = new ArrayList<>();
                fragments.add(currentText);
                mergedIndexes.add(i);

                // 向下寻找相邻的产品名称片段
                for (int j = i + 1; j < texts.size(); j++) {
                    if (mergedIndexes.contains(j)) {
                        continue;
                    }

                    OcrTextResult nextText = texts.get(j);

                    // 检查垂直距离和内容特征
                    if (isAdjacentProductFragment(currentText, nextText) &&
                            isProductNameFragment(nextText.getText())) {
                        fragments.add(nextText);
                        mergedIndexes.add(j);
                    }
                }

                // 如果找到了多个片段，合并它们
                if (fragments.size() > 1) {
                    OcrTextResult mergedText = mergeTextFragments(fragments);
                    result.add(mergedText);
                    log.debug("合并产品名称: {} 个片段 -> '{}'", fragments.size(), mergedText.getText());
                } else {
                    result.add(currentText);
                }
            } else {
                result.add(currentText);
            }
        }

        log.debug("产品名称合并完成: {} -> {}", texts.size(), result.size());
        return result;
    }

    /**
     * 判断是否是产品名称片段
     */
    private boolean isProductNameFragment(String text) {
        if (StringUtils.isBlank(text) || text.length() < 3) {
            return false;
        }

        // 产品名称常见片段特征
        return text.matches(".*[理财|基金|债券|产品|收益|固定|开放|净值|天天|添益|核心|优选|持盈|银理财].*") ||
                text.matches(".*[工银|交银|兴银|平安|招商|中信|建信|华夏].*") ||
                text.matches(".*[·|\\|].*");
    }

    /**
     * 判断是否是相邻的产品片段
     */
    private boolean isAdjacentProductFragment(OcrTextResult text1, OcrTextResult text2) {
        // 检查垂直距离
        double verticalDistance = Math.abs(getCenterY(text2.getBbox()) - getCenterY(text1.getBbox()));

        // 检查水平重叠度
        double overlap = calculateHorizontalOverlap(text1.getBbox(), text2.getBbox());

        return verticalDistance <= PRODUCT_NAME_MERGE_DISTANCE && overlap > 0.3;
    }

    /**
     * 安全获取centerY，如果为null则计算
     */
    private double getCenterY(OcrTextResult.BoundingBox bbox) {
        if (bbox.getCenterY() != null) {
            return bbox.getCenterY();
        }
        return (bbox.getTop() + bbox.getBottom()) / 2.0;
    }

    /**
     * 安全获取centerX，如果为null则计算
     */
    private double getCenterX(OcrTextResult.BoundingBox bbox) {
        if (bbox.getCenterX() != null) {
            return bbox.getCenterX();
        }
        return (bbox.getLeft() + bbox.getRight()) / 2.0;
    }

    /**
     * 计算水平重叠度
     */
    private double calculateHorizontalOverlap(OcrTextResult.BoundingBox bbox1, OcrTextResult.BoundingBox bbox2) {
        double left = Math.max(bbox1.getLeft(), bbox2.getLeft());
        double right = Math.min(bbox1.getRight(), bbox2.getRight());

        if (left >= right) {
            return 0.0;
        }

        double overlapWidth = right - left;
        double minWidth = Math.min(bbox1.getRight() - bbox1.getLeft(), bbox2.getRight() - bbox2.getLeft());

        return overlapWidth / minWidth;
    }

    /**
     * 合并文本片段 - 修复版
     */
    private OcrTextResult mergeTextFragments(List<OcrTextResult> fragments) {
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
     * 修复的文本处理和分类
     */
    private ProcessedTexts processAndClassifyTextsFixed(List<OcrTextResult> texts) {
        ProcessedTexts result = new ProcessedTexts();

        for (OcrTextResult text : texts) {
            String content = text.getText().trim();

            if (StringUtils.isBlank(content)) {
                continue;
            }

            // 严格的金额识别
            if (isValidAmountTextStrict(content)) {
                BigDecimal amount = parseAmountStrict(content);
                if (amount != null && isValidAmountRange(amount)) {
                    result.getAmountTexts().add(text);
                    log.debug("识别有效金额: '{}' -> {}", content, amount);
                    continue;
                }
            }

            // 过滤无关文本
            if (isIrrelevantTextStrict(content)) {
                result.getIrrelevantTexts().add(text);
                log.debug("过滤无关文本: '{}'", content);
                continue;
            }

            // 其余作为内容文本
            if (content.length() >= MIN_CONTENT_LENGTH && isMeaningfulContent(content)) {
                result.getContentTexts().add(text);
                log.debug("识别内容文本: '{}'", content);
            }
        }

        return result;
    }

    /**
     * 严格的金额文本判断
     */
    private boolean isValidAmountTextStrict(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 排除日期格式
        if (trimmed.matches("^\\d{4}[/-]\\d{2}[/-]\\d{2}$")) {
            log.debug("排除日期格式: {}", trimmed);
            return false;
        }

        // 排除时间格式
        if (trimmed.matches("^\\d{2}:\\d{2}(:\\d{2})?$")) {
            log.debug("排除时间格式: {}", trimmed);
            return false;
        }

        // 排除包含中文的
        if (trimmed.matches(".*[\\u4e00-\\u9fa5].*")) {
            return false;
        }

        // 排除包含字母的
        if (trimmed.matches(".*[a-zA-Z].*")) {
            return false;
        }

        // 严格的金额格式
        // 1. 标准金额格式：123,456.78
        if (trimmed.matches("^\\d{1,3}(,\\d{3})*(\\.\\d{2})?$")) {
            return true;
        }

        // 2. 简单小数：123.45, 1234.56
        if (trimmed.matches("^\\d+\\.\\d{1,4}$")) {
            return true;
        }

        // 3. 整数金额（但不能太长，避免误识别日期）
        if (trimmed.matches("^\\d{1,8}$")) {
            try {
                long value = Long.parseLong(trimmed);
                return value >= 1 && value <= 99999999; // 合理的金额范围
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 严格的金额解析
     */
    private BigDecimal parseAmountStrict(String content) {
        try {
            String cleaned = content.trim().replace(",", "");
            BigDecimal amount = new BigDecimal(cleaned);

            // 验证金额合理性
            if (isValidAmountRange(amount)) {
                return amount;
            }

        } catch (NumberFormatException e) {
            log.debug("金额解析失败: {}", content);
        }

        return null;
    }

    /**
     * 验证金额范围
     */
    private boolean isValidAmountRange(BigDecimal amount) {
        return amount != null &&
                amount.compareTo(BigDecimal.valueOf(MIN_VALID_AMOUNT)) >= 0 &&
                amount.compareTo(BigDecimal.valueOf(MAX_VALID_AMOUNT)) <= 0;
    }

    /**
     * 判断是否为有意义的内容
     */
    private boolean isMeaningfulContent(String content) {
        // 排除纯符号
        if (content.matches("^[^\\p{L}\\p{N}]+$")) {
            return false;
        }

        // 排除单个字符（除非是中文）
        if (content.length() == 1 && !content.matches("[\\u4e00-\\u9fa5]")) {
            return false;
        }

        // 排除明显的无意义文本
        if (content.matches("^[\\s\\-_=.]{2,}$")) {
            return false;
        }

        return true;
    }

    /**
     * 分析列边界
     */
    private ColumnBoundary analyzeColumnBoundary(ProcessedTexts texts, double pageWidth) {
        ColumnBoundary boundary = new ColumnBoundary();

        if (!texts.getAmountTexts().isEmpty() && !texts.getContentTexts().isEmpty()) {
            double avgAmountX = texts.getAmountTexts().stream()
                    .mapToDouble(t -> t.getBbox().getLeft())
                    .average()
                    .orElse(pageWidth * 0.7);

            double avgContentX = texts.getContentTexts().stream()
                    .mapToDouble(t -> t.getBbox().getLeft())
                    .average()
                    .orElse(pageWidth * 0.1);

            boundary.setBoundary((avgAmountX + avgContentX) / 2);
        } else {
            boundary.setBoundary(pageWidth / 2);
        }

        log.debug("列边界分析: 金额平均X={}, 内容平均X={}, 边界={}",
                texts.getAmountTexts().stream().mapToDouble(t -> t.getBbox().getLeft()).average().orElse(0),
                texts.getContentTexts().stream().mapToDouble(t -> t.getBbox().getLeft()).average().orElse(0),
                boundary.getBoundary());

        return boundary;
    }

    /**
     * 修复的产品构建
     */
    private List<ProductItem> buildProductsFromAmountsFixed(ProcessedTexts texts, ColumnBoundary columnBoundary) {
        List<ProductItem> products = new ArrayList<>();
        Set<OcrTextResult> usedTexts = new HashSet<>();

        // 按Y坐标排序金额
        List<OcrTextResult> sortedAmounts = texts.getAmountTexts().stream()
                .sorted(Comparator.comparingDouble(t -> getCenterY(t.getBbox())))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedAmounts.size(); i++) {
            OcrTextResult amountText = sortedAmounts.get(i);

            if (usedTexts.contains(amountText)) {
                continue;
            }

            BigDecimal amount = parseAmountStrict(amountText.getText().trim());
            if (amount == null || !isValidAmountRange(amount)) {
                continue;
            }

            log.debug("处理金额: '{}' (Y={}, 金额={})",
                    amountText.getText(), getCenterY(amountText.getBbox()), amount);

            // 为该金额找到相关的内容文本
            List<OcrTextResult> relatedTexts = findRelatedTextsFixed(amountText, texts.getContentTexts(), usedTexts);

            if (relatedTexts.isEmpty()) {
                log.debug("金额 '{}' 没有找到相关内容文本", amountText.getText());
                // 创建一个最小产品项
                ProductItem minimalProduct = createMinimalProduct(amountText, amount, i);
                if (minimalProduct != null) {
                    products.add(minimalProduct);
                    usedTexts.add(amountText);
                }
                continue;
            }

            // 构建产品项
            ProductItem product = buildProductFixed(amountText, amount, relatedTexts, i);
            if (product != null) {
                products.add(product);
                usedTexts.add(amountText);
                usedTexts.addAll(relatedTexts);
                log.info("构建产品成功: '{}' - {}",
                        product.getCleanName(), textAnalysisUtil.formatAmountDisplay(amount));
            }
        }

        return products;
    }

    /**
     * 修复的相关文本查找 - 解决NullPointerException
     */
    private List<OcrTextResult> findRelatedTextsFixed(OcrTextResult amountText,
                                                      List<OcrTextResult> contentTexts,
                                                      Set<OcrTextResult> usedTexts) {
        double amountY = getCenterY(amountText.getBbox()); // 使用安全的getCenterY方法
        List<OcrTextResult> candidates = new ArrayList<>();

        // 第一阶段：查找同行文本
        for (OcrTextResult contentText : contentTexts) {
            if (usedTexts.contains(contentText)) {
                continue;
            }

            double contentY = getCenterY(contentText.getBbox()); // 使用安全的getCenterY方法
            double distance = Math.abs(amountY - contentY);

            if (distance <= SAME_ROW_TOLERANCE) {
                candidates.add(contentText);
                log.debug("找到同行文本: '{}' (距离={})", contentText.getText(), distance);
            }
        }

        // 第二阶段：如果同行文本不足，查找邻近行文本
        if (candidates.isEmpty()) {
            for (OcrTextResult contentText : contentTexts) {
                if (usedTexts.contains(contentText)) {
                    continue;
                }

                double contentY = getCenterY(contentText.getBbox()); // 使用安全的getCenterY方法
                double distance = Math.abs(amountY - contentY);

                if (distance <= NEARBY_ROW_TOLERANCE) {
                    candidates.add(contentText);
                    log.debug("找到邻近文本: '{}' (距离={})", contentText.getText(), distance);
                }
            }
        }

        // 按距离排序，选择最相关的文本
        candidates.sort(Comparator.comparingDouble(t -> Math.abs(getCenterY(t.getBbox()) - amountY)));

        // 返回最相关的1-3个文本
        return candidates.stream().limit(3).collect(Collectors.toList());
    }

    /**
     * 创建最小产品项
     */
    private ProductItem createMinimalProduct(OcrTextResult amountText, BigDecimal amount, int index) {
        ProductItem product = new ProductItem();

        if (amount.scale() < 2) {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        }
        product.setAmountText(amountText);
        product.setAmount(amount);

        String defaultName = "理财产品 " + (index + 1);
        product.setOriginalName(defaultName);
        product.setCleanName(defaultName);
        product.setRelatedTexts(Collections.emptyList());

        product.setPagePosition(getCenterY(amountText.getBbox()));
        product.setRowIndex(index);
        product.setConfidence(amountText.getConfidence());

        return product;
    }

    /**
     * 判断是否为产品名称候选
     */
    private boolean isProductNameCandidate(String text) {
        // 产品名称应该包含有意义的词汇
        return text.matches(".*[理财|基金|债券|产品|收益|固定|开放|净值|天天|添益|核心|优选|持盈].*") ||
                text.matches(".*[工银|交银|兴银|平安|招商|中信|建信|华夏].*") ||
                text.length() >= 8; // 长度足够的文本也可能是产品名称
    }

    /**
     * 验证和排序产品
     */
    private List<ProductItem> validateAndSortProducts(List<ProductItem> products) {
        return products.stream()
                .filter(this::isValidProduct)
                .sorted(Comparator.comparingDouble(ProductItem::getPagePosition))
                .collect(Collectors.toList());
    }

    /**
     * 验证产品有效性
     */
    private boolean isValidProduct(ProductItem product) {
        // 验证产品名称
        if (StringUtils.isBlank(product.getCleanName())) {
            return false;
        }

        // 验证金额
        if (product.getAmount() == null || !isValidAmountRange(product.getAmount())) {
            return false;
        }

        return true;
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
            log.info("  {}. '{}' - {} (Y位置: {}, 置信度: {})",
                    i + 1, p.getCleanName(), formattedAmount, p.getPagePosition(), p.getConfidence());
        }
    }

    @Getter
    private static class ProcessedTexts {
        private final List<OcrTextResult> amountTexts = new ArrayList<>();
        private final List<OcrTextResult> contentTexts = new ArrayList<>();
        private final List<OcrTextResult> irrelevantTexts = new ArrayList<>();
    }

    @Setter
    @Getter
    private static class ColumnBoundary {
        private double boundary;
    }
}