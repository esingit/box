package com.esin.box.service.processor;

import com.esin.box.dto.ocr.OcrTextResult;
import com.esin.box.dto.ocr.PageLayout;
import com.esin.box.dto.ocr.ProductItem;
import com.esin.box.utils.TextAnalysisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下行布局处理器 - 修复版
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RowLayoutProcessor implements LayoutProcessor {

    private final TextAnalysisUtil textAnalysisUtil;

    public static class LayoutConfig {
        public static final double PRODUCT_SEARCH_RANGE = 200.0;
        public static final double AMOUNT_SEARCH_RANGE = 150.0;
        public static final double VERTICAL_TOLERANCE = 80.0;
        public static final double MIN_VALID_AMOUNT = 0.01;
        public static final double MAX_VALID_AMOUNT = 100000000.0;
        public static final int MIN_PRODUCT_NAME_LENGTH = 6;
        public static final int MAX_PRODUCT_NAME_LENGTH = 100;
        public static final double PRODUCT_NAME_MERGE_DISTANCE = 50.0;
        public static final double SAME_PRODUCT_Y_TOLERANCE = 300.0; // 同一产品的Y坐标容差
    }

    private double getCenterY(OcrTextResult.BoundingBox bbox) {
        if (bbox.getCenterY() != null) {
            return bbox.getCenterY();
        }
        return (bbox.getTop() + bbox.getBottom()) / 2.0;
    }

    private double getCenterX(OcrTextResult.BoundingBox bbox) {
        if (bbox.getCenterX() != null) {
            return bbox.getCenterX();
        }
        return (bbox.getLeft() + bbox.getRight()) / 2.0;
    }

    @Override
    public List<ProductItem> processLayout(PageLayout layout) {
        log.info("=== 开始上下行布局处理（修复版） ===");
        log.info("有效文本数量: {}", layout.getValidTexts().size());

        try {
            // 1. 预处理：合并被拆分的产品名称
            List<OcrTextResult> mergedTexts = mergeFragmentedProductNames(layout.getValidTexts());
            log.info("文本合并后数量: {}", mergedTexts.size());

            // 2. 识别产品名称文本（修复版）
            List<OcrTextResult> productNameTexts = identifyProductNamesFixed(mergedTexts);
            log.info("识别到产品名称数量: {}", productNameTexts.size());

            List<ProductItem> products = new ArrayList<>();

            if (!productNameTexts.isEmpty()) {
                // 策略1: 基于产品名称创建组 - 处理重复产品
                products = processWithProductNameStrategyDedup(mergedTexts, productNameTexts);
                log.info("产品名称策略构建产品数量: {}", products.size());
            }

            // 策略2: 如果产品数量不够，尝试金额驱动策略
            if (products.size() < 2) {
                log.info("产品数量不足，尝试金额驱动策略");
                List<ProductItem> amountDrivenProducts = processWithAmountDrivenStrategy(mergedTexts);
                if (amountDrivenProducts.size() > products.size()) {
                    products = amountDrivenProducts;
                }
            }

            // 后处理：验证和去重
            products = postProcessProductsFixed(products);

            log.info("上下行布局处理完成，最终产品数量: {}", products.size());
            logProductDetails(products);

            return products;

        } catch (Exception e) {
            log.error("上下行布局处理异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 基于产品名称策略处理 - 增加去重逻辑
     */
    private List<ProductItem> processWithProductNameStrategyDedup(List<OcrTextResult> allTexts,
                                                                  List<OcrTextResult> productNameTexts) {
        List<ProductItem> products = new ArrayList<>();
        Set<OcrTextResult> usedAmounts = new HashSet<>();

        // 按产品名称分组，处理重复的产品
        Map<String, List<OcrTextResult>> productGroups = new LinkedHashMap<>();
        for (OcrTextResult productNameText : productNameTexts) {
            String cleanName = textAnalysisUtil.cleanProductName(productNameText.getText());
            productGroups.computeIfAbsent(cleanName, k -> new ArrayList<>()).add(productNameText);
        }

        int index = 0;
        for (Map.Entry<String, List<OcrTextResult>> entry : productGroups.entrySet()) {
            List<OcrTextResult> sameProducts = entry.getValue();

            for (OcrTextResult productName : sameProducts) {
                // 寻找对应的金额
                List<OcrTextResult> relatedAmounts = findRelatedAmountsFixed(allTexts, productName, usedAmounts);

                if (!relatedAmounts.isEmpty()) {
                    OcrTextResult amountText = relatedAmounts.get(0);
                    BigDecimal amount = parseAmountFlexible(amountText.getText());

                    if (amount != null && isValidAmountRange(amount)) {
                        ProductItem product = buildProductFromNameAndAmount(productName, amountText, amount, index++);
                        if (product != null) {
                            products.add(product);
                            usedAmounts.add(amountText);
                            log.info("构建产品成功: '{}' - {}",
                                    product.getCleanName(), textAnalysisUtil.formatAmountDisplay(amount));
                        }
                    }
                }
            }
        }

        return products;
    }

    /**
     * 灵活的金额解析 - 处理错误的小数点格式
     */
    private BigDecimal parseAmountFlexible(String content) {
        try {
            String cleaned = content.trim();

            // 处理错误的小数点格式，如 12.041.40 -> 12,041.40
            if (cleaned.matches("\\d+\\.\\d{3}\\.\\d{2}")) {
                // 替换第一个小数点为逗号
                int firstDot = cleaned.indexOf('.');
                if (firstDot > 0) {
                    cleaned = cleaned.substring(0, firstDot) + "," + cleaned.substring(firstDot + 1);
                }
            }

            // 移除逗号
            cleaned = cleaned.replace(",", "");

            BigDecimal amount = new BigDecimal(cleaned);
            return isValidAmountRange(amount) ? amount : null;
        } catch (NumberFormatException e) {
            log.debug("金额解析失败: {}", content);
            return null;
        }
    }

    /**
     * 严格的金额文本判断 - 改进版
     */
    private boolean isValidAmountTextStrict(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 排除日期格式
        if (trimmed.matches("^\\d{4}[/-]\\d{2}[/-]\\d{2}$")) {
            return false;
        }

        // 排除包含中文的
        if (trimmed.matches(".*[\\u4e00-\\u9fa5].*")) {
            return false;
        }

        // 标准金额格式（包括错误的小数点格式）
        if (trimmed.matches("^\\d{1,3}(,\\d{3})*(\\.\\d{2})?$") ||
                trimmed.matches("^\\d+\\.\\d{1,4}$") ||
                trimmed.matches("^\\d{1,8}$") ||
                trimmed.matches("^\\d+\\.\\d{3}\\.\\d{2}$")) { // 处理 12.041.40 这种格式

            BigDecimal amount = parseAmountFlexible(trimmed);
            return amount != null && isValidAmountRange(amount);
        }

        return false;
    }

    /**
     * 修复的产品名称识别 - 排除更多元数据
     */
    private List<OcrTextResult> identifyProductNamesFixed(List<OcrTextResult> texts) {
        List<OcrTextResult> productNames = new ArrayList<>();

        for (OcrTextResult text : texts) {
            if (isValidProductNameFixed(text.getText())) {
                productNames.add(text);
                log.debug("识别到产品名称: '{}'", text.getText());
            }
        }

        // 如果没有识别到，使用更宽松的条件
        if (productNames.isEmpty()) {
            for (OcrTextResult text : texts) {
                if (isProductNameCandidateLoose(text.getText())) {
                    productNames.add(text);
                    log.debug("宽松条件识别产品名称: '{}'", text.getText());
                }
            }
        }

        return productNames;
    }

    /**
     * 严格的元数据判断 - 增加更多排除项
     */
    private boolean isStrictMetadata(String content) {
        Set<String> strictMetadata = Set.of(
                "产品持仓", "撤单", "持仓市值", "持仓币值", "可赎回日", "赎回类型", "赎回尖型",
                "今日可赎", "每日可赎", "预约赎回", "周期结束日", "可赎回开始日",
                "总金额", "总金额（元）", "名称", "金额", "理财", "收起", "展开",
                "温馨提示", "（元）", "日期", "时间", "余额", "总计", "合计",
                "最短持有期内不可赎回，可赎回开始日起每日可赎"
        );
        return strictMetadata.contains(content.trim());
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
                List<OcrTextResult> fragments = collectProductFragments(texts, i, mergedIndexes);

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

        return result;
    }

    /**
     * 收集产品片段
     */
    private List<OcrTextResult> collectProductFragments(List<OcrTextResult> texts, int startIndex, Set<Integer> mergedIndexes) {
        List<OcrTextResult> fragments = new ArrayList<>();
        OcrTextResult currentText = texts.get(startIndex);
        fragments.add(currentText);
        mergedIndexes.add(startIndex);

        // 向下寻找相邻的产品名称片段
        double currentY = getCenterY(currentText.getBbox());
        double currentLeft = currentText.getBbox().getLeft();
        double currentRight = currentText.getBbox().getRight();

        for (int j = startIndex + 1; j < texts.size(); j++) {
            if (mergedIndexes.contains(j)) {
                continue;
            }

            OcrTextResult nextText = texts.get(j);
            double nextY = getCenterY(nextText.getBbox());

            // 检查垂直距离
            if (Math.abs(nextY - currentY) > LayoutConfig.PRODUCT_NAME_MERGE_DISTANCE) {
                continue;
            }

            // 检查水平重叠或连续性
            double nextLeft = nextText.getBbox().getLeft();
            double nextRight = nextText.getBbox().getRight();

            boolean isAdjacent = (nextLeft <= currentRight + 20) && (nextRight >= currentLeft - 20);

            if (isAdjacent && isProductNameFragment(nextText.getText())) {
                fragments.add(nextText);
                mergedIndexes.add(j);

                // 更新边界
                currentLeft = Math.min(currentLeft, nextLeft);
                currentRight = Math.max(currentRight, nextRight);
                currentY = nextY; // 移动到下一行
            }
        }

        return fragments;
    }

    /**
     * 判断是否是产品名称片段
     */
    private boolean isProductNameFragment(String text) {
        if (StringUtils.isBlank(text) || text.length() < 2) {
            return false;
        }

        // 排除明显的非产品名称文本
        if (isStrictMetadata(text)) {
            return false;
        }

        // 产品名称常见特征
        return text.matches(".*[理财|基金|债券|产品|收益|固定|开放|净值|天天|添益|核心|优选|持盈|银理财|个理].*") ||
                text.matches(".*[工银|交银|兴银|平安|招商|中信|建信|华夏].*") ||
                text.matches(".*[·|\\||款|号].*") ||
                (text.length() >= 4 && text.matches(".*[\\u4e00-\\u9fa5].*"));
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
     * 修复的产品名称有效性判断
     */
    private boolean isValidProductNameFixed(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 长度检查
        if (trimmed.length() < LayoutConfig.MIN_PRODUCT_NAME_LENGTH ||
                trimmed.length() > LayoutConfig.MAX_PRODUCT_NAME_LENGTH) {
            return false;
        }

        // 排除金额格式
        if (isValidAmountTextStrict(trimmed)) {
            return false;
        }

        // 排除日期格式
        if (trimmed.matches("^\\d{4}[/-]\\d{2}[/-]\\d{2}$")) {
            return false;
        }

        // 排除严格的元数据
        if (isStrictMetadata(trimmed)) {
            return false;
        }

        // 产品名称特征检查
        boolean hasProductKeywords = trimmed.contains("理财") || trimmed.contains("基金") ||
                trimmed.contains("债券") || trimmed.contains("产品") ||
                trimmed.contains("收益") || trimmed.contains("固定") ||
                trimmed.contains("开放") || trimmed.contains("净值") ||
                trimmed.contains("天天") || trimmed.contains("添益") ||
                trimmed.contains("核心") || trimmed.contains("优选") ||
                trimmed.contains("持盈");

        boolean hasInstitutionNames = trimmed.contains("工银") || trimmed.contains("交银") ||
                trimmed.contains("兴银") || trimmed.contains("平安") ||
                trimmed.contains("招商") || trimmed.contains("中信") ||
                trimmed.contains("建信") || trimmed.contains("华夏");

        boolean hasStructuralChars = trimmed.contains("·") || trimmed.contains("|") ||
                trimmed.contains("号") || trimmed.contains("款");

        return hasProductKeywords || hasInstitutionNames ||
                (hasStructuralChars && trimmed.length() >= 8);
    }

    /**
     * 宽松的产品名称候选判断
     */
    private boolean isProductNameCandidateLoose(String content) {
        if (StringUtils.isBlank(content) || content.length() < 4) {
            return false;
        }

        // 排除明显的非产品文本
        if (isValidAmountTextStrict(content) || isStrictMetadata(content)) {
            return false;
        }

        // 包含中文且长度合理
        return content.matches(".*[\\u4e00-\\u9fa5].*") &&
                content.length() >= 6 &&
                !content.matches(".*(日期|时间|余额|总计|合计).*");
    }

    /**
     * 基于产品名称策略处理
     */
    private List<ProductItem> processWithProductNameStrategy(List<OcrTextResult> allTexts,
                                                             List<OcrTextResult> productNameTexts) {
        List<ProductItem> products = new ArrayList<>();
        Set<OcrTextResult> usedAmounts = new HashSet<>();

        for (int i = 0; i < productNameTexts.size(); i++) {
            OcrTextResult productName = productNameTexts.get(i);

            // 寻找对应的金额
            List<OcrTextResult> relatedAmounts = findRelatedAmountsFixed(allTexts, productName, usedAmounts);

            if (!relatedAmounts.isEmpty()) {
                OcrTextResult amountText = relatedAmounts.get(0);
                BigDecimal amount = parseAmountStrict(amountText.getText());

                if (amount != null && isValidAmountRange(amount)) {
                    ProductItem product = buildProductFromNameAndAmount(productName, amountText, amount, i);
                    if (product != null) {
                        products.add(product);
                        usedAmounts.add(amountText);
                        log.info("构建产品成功: '{}' - {}",
                                product.getCleanName(), textAnalysisUtil.formatAmountDisplay(amount));
                    }
                }
            }
        }

        return products;
    }

    /**
     * 金额驱动策略处理
     */
    private List<ProductItem> processWithAmountDrivenStrategy(List<OcrTextResult> allTexts) {
        List<ProductItem> products = new ArrayList<>();
        Set<OcrTextResult> usedTexts = new HashSet<>();

        // 识别所有有效金额
        List<OcrTextResult> amountTexts = allTexts.stream()
                .filter(text -> isValidAmountTextStrict(text.getText()))
                .sorted(Comparator.comparingDouble(t -> getCenterY(t.getBbox())))
                .collect(Collectors.toList());

        log.info("识别到有效金额数量: {}", amountTexts.size());

        for (int i = 0; i < amountTexts.size(); i++) {
            OcrTextResult amountText = amountTexts.get(i);

            if (usedTexts.contains(amountText)) {
                continue;
            }

            BigDecimal amount = parseAmountStrict(amountText.getText());
            if (amount == null || !isValidAmountRange(amount)) {
                continue;
            }

            // 寻找相关的产品名称文本
            List<OcrTextResult> relatedTexts = findRelatedProductTexts(allTexts, amountText, usedTexts);

            ProductItem product = buildProductFromAmountAndTexts(amountText, amount, relatedTexts, i);
            if (product != null) {
                products.add(product);
                usedTexts.add(amountText);
                usedTexts.addAll(relatedTexts);
            }
        }

        return products;
    }

    /**
     * 寻找相关金额（修复版）
     */
    private List<OcrTextResult> findRelatedAmountsFixed(List<OcrTextResult> allTexts,
                                                        OcrTextResult productName,
                                                        Set<OcrTextResult> usedAmounts) {
        double productY = getCenterY(productName.getBbox());

        return allTexts.stream()
                .filter(text -> !text.equals(productName))
                .filter(text -> !usedAmounts.contains(text))
                .filter(text -> isValidAmountTextStrict(text.getText()))
                .filter(text -> {
                    double distance = Math.abs(getCenterY(text.getBbox()) - productY);
                    return distance <= LayoutConfig.AMOUNT_SEARCH_RANGE;
                })
                .sorted(Comparator.comparingDouble(t -> Math.abs(getCenterY(t.getBbox()) - productY)))
                .collect(Collectors.toList());
    }

    /**
     * 寻找相关产品文本
     */
    private List<OcrTextResult> findRelatedProductTexts(List<OcrTextResult> allTexts,
                                                        OcrTextResult amountText,
                                                        Set<OcrTextResult> usedTexts) {
        double amountY = getCenterY(amountText.getBbox());

        return allTexts.stream()
                .filter(text -> !text.equals(amountText))
                .filter(text -> !usedTexts.contains(text))
                .filter(text -> !isValidAmountTextStrict(text.getText()))
                .filter(text -> !isStrictMetadata(text.getText()))
                .filter(text -> {
                    double distance = Math.abs(getCenterY(text.getBbox()) - amountY);
                    return distance <= LayoutConfig.VERTICAL_TOLERANCE;
                })
                .sorted(Comparator.comparingDouble(t -> Math.abs(getCenterY(t.getBbox()) - amountY)))
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 严格的金额解析
     */
    private BigDecimal parseAmountStrict(String content) {
        try {
            String cleaned = content.trim().replace(",", "");
            BigDecimal amount = new BigDecimal(cleaned);
            return isValidAmountRange(amount) ? amount : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 验证金额范围
     */
    private boolean isValidAmountRange(BigDecimal amount) {
        return amount != null &&
                amount.compareTo(BigDecimal.valueOf(LayoutConfig.MIN_VALID_AMOUNT)) >= 0 &&
                amount.compareTo(BigDecimal.valueOf(LayoutConfig.MAX_VALID_AMOUNT)) <= 0;
    }

    /**
     * 从产品名称和金额构建产品
     */
    private ProductItem buildProductFromNameAndAmount(OcrTextResult productNameText,
                                                      OcrTextResult amountText,
                                                      BigDecimal amount,
                                                      int index) {
        ProductItem product = new ProductItem();

        product.setAmountText(amountText);
        product.setAmount(amount);

        String originalName = productNameText.getText().trim();
        product.setOriginalName(originalName);
        product.setCleanName(textAnalysisUtil.cleanProductName(originalName));

        List<OcrTextResult> relatedTexts = Arrays.asList(productNameText);
        product.setRelatedTexts(relatedTexts);

        List<OcrTextResult> allTexts = Arrays.asList(productNameText, amountText);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));
        product.setPagePosition(getCenterY(productNameText.getBbox()));
        product.setRowIndex(index);

        return product;
    }

    /**
     * 从金额和相关文本构建产品
     */
    private ProductItem buildProductFromAmountAndTexts(OcrTextResult amountText,
                                                       BigDecimal amount,
                                                       List<OcrTextResult> relatedTexts,
                                                       int index) {
        ProductItem product = new ProductItem();

        product.setAmountText(amountText);
        product.setAmount(amount);

        String productName;
        if (relatedTexts.isEmpty()) {
            productName = "理财产品 " + (index + 1);
        } else {
            productName = relatedTexts.stream()
                    .map(OcrTextResult::getText)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(""));

            if (StringUtils.isBlank(productName) || productName.length() < 4) {
                productName = "理财产品 " + (index + 1);
            }
        }

        product.setOriginalName(productName);
        product.setCleanName(textAnalysisUtil.cleanProductName(productName));
        product.setRelatedTexts(relatedTexts);

        List<OcrTextResult> allTexts = new ArrayList<>(relatedTexts);
        allTexts.add(amountText);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));
        product.setPagePosition(getCenterY(amountText.getBbox()));
        product.setRowIndex(index);

        return product;
    }

    /**
     * 修复的后处理
     */
    private List<ProductItem> postProcessProductsFixed(List<ProductItem> products) {
        // 去重
        List<ProductItem> deduplicated = deduplicateProductsFixed(products);

        // 验证
        List<ProductItem> validated = validateProductsFixed(deduplicated);

        // 排序
        validated.sort(Comparator.comparingDouble(p ->
                p.getAmountText() != null ? getCenterY(p.getAmountText().getBbox()) : 0));

        return validated;
    }

    /**
     * 修复的去重
     */
    private List<ProductItem> deduplicateProductsFixed(List<ProductItem> products) {
        List<ProductItem> result = new ArrayList<>();
        Set<String> seenAmounts = new HashSet<>();

        for (ProductItem product : products) {
            if (product.getAmount() == null) {
                continue;
            }

            String amountKey = product.getAmount().toString();
            if (!seenAmounts.contains(amountKey)) {
                result.add(product);
                seenAmounts.add(amountKey);
            } else {
                log.debug("去重产品: {} (金额重复: {})", product.getCleanName(), amountKey);
            }
        }

        return result;
    }

    /**
     * 修复的验证
     */
    private List<ProductItem> validateProductsFixed(List<ProductItem> products) {
        return products.stream()
                .filter(product -> {
                    // 验证产品名称
                    if (StringUtils.isBlank(product.getCleanName()) ||
                            product.getCleanName().length() < 3) {
                        return false;
                    }

                    // 验证金额
                    if (product.getAmount() == null ||
                            !isValidAmountRange(product.getAmount())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
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

    // 内部类保持不变...
    private static class ProductTextAnalysis {
        private final List<OcrTextResult> productNameTexts = new ArrayList<>();
        private final List<OcrTextResult> amountTexts = new ArrayList<>();
        private final List<OcrTextResult> metadataTexts = new ArrayList<>();
        private final List<OcrTextResult> otherTexts = new ArrayList<>();

        public List<OcrTextResult> getProductNameTexts() {
            return productNameTexts;
        }

        public List<OcrTextResult> getAmountTexts() {
            return amountTexts;
        }

        public List<OcrTextResult> getMetadataTexts() {
            return metadataTexts;
        }

        public List<OcrTextResult> getOtherTexts() {
            return otherTexts;
        }
    }

    public static class VerticalGroup {
        private double centerX;
        private double topY;
        private double bottomY;
        private final List<OcrTextResult> texts = new ArrayList<>();

        public double getCenterX() {
            return centerX;
        }

        public void setCenterX(double centerX) {
            this.centerX = centerX;
        }

        public double getTopY() {
            return topY;
        }

        public void setTopY(double topY) {
            this.topY = topY;
        }

        public double getBottomY() {
            return bottomY;
        }

        public void setBottomY(double bottomY) {
            this.bottomY = bottomY;
        }

        public List<OcrTextResult> getTexts() {
            return texts;
        }
    }
}