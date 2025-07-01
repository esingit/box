package com.esin.box.service.processor;

import com.esin.box.dto.ocr.*;
import com.esin.box.utils.TextAnalysisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下行布局处理器 - 完全优化版
 * 专门处理上下行结构的文档布局，基于产品名称进行智能分组
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RowLayoutProcessor implements LayoutProcessor {

    private final TextAnalysisUtil textAnalysisUtil;

    /**
     * 布局配置常量
     */
    public static class LayoutConfig {
        public static final int MIN_VERTICAL_GROUP_SIZE = 2;
        public static final double ROW_HEIGHT_TOLERANCE = 50.0;
        public static final double PRODUCT_SEARCH_RANGE = 200.0;
        public static final double METADATA_DISTANCE_THRESHOLD = 60.0;
        public static final int MIN_PRODUCT_NAME_LENGTH = 8;
        public static final int MAX_PRODUCT_NAME_LENGTH = 100;
        public static final double VERTICAL_TOLERANCE = 80.0;
        public static final double AMOUNT_SEARCH_RANGE = 150.0;
    }

    @Override
    public List<ProductItem> processLayout(PageLayout layout) {
        log.info("=== 开始上下行布局处理 ===");
        log.info("有效文本数量: {}", layout.getValidTexts().size());

        try {
            // 1. 识别产品名称文本（使用更宽松的条件）
            List<OcrTextResult> productNameTexts = identifyProductNames(layout.getValidTexts());
            log.info("识别到产品名称数量: {}", productNameTexts.size());

            if (productNameTexts.isEmpty()) {
                log.warn("未识别到产品名称，尝试传统分组方法");
                return processWithTraditionalGrouping(layout);
            }

            // 2. 基于产品名称创建垂直组
            List<VerticalGroup> verticalGroups = createGroupsAroundProducts(layout, productNameTexts);
            log.info("基于产品名称创建垂直组: {}", verticalGroups.size());

            // 3. 处理每个垂直组
            List<ProductItem> products = new ArrayList<>();
            for (int i = 0; i < verticalGroups.size(); i++) {
                VerticalGroup group = verticalGroups.get(i);
                ProductItem product = buildProductFromVerticalGroup(group, i);
                if (product != null) {
                    products.add(product);
                    log.info("垂直组 {} 构建产品成功: '{}' - {}",
                            i, product.getCleanName(), product.getAmount());
                } else {
                    log.debug("垂直组 {} 构建产品失败", i);
                }
            }

            log.info("上下行布局处理完成，产品数量: {}", products.size());
            return products;

        } catch (Exception e) {
            log.error("上下行布局处理异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 识别产品名称文本 - 使用更宽松的条件
     */
    private List<OcrTextResult> identifyProductNames(List<OcrTextResult> texts) {
        List<OcrTextResult> productNames = texts.stream()
                .filter(this::isProductNameText)
                .collect(Collectors.toList());

        log.debug("产品名称识别详情:");
        for (OcrTextResult text : productNames) {
            log.debug("  - 产品名称: '{}'", text.getText());
        }

        // 如果使用严格条件没找到，尝试宽松条件
        if (productNames.isEmpty()) {
            log.debug("严格条件未找到产品名称，尝试宽松条件");
            productNames = texts.stream()
                    .filter(this::isProductNameTextLoose)
                    .collect(Collectors.toList());

            log.debug("宽松条件下的产品名称:");
            for (OcrTextResult text : productNames) {
                log.debug("  - 产品名称: '{}'", text.getText());
            }
        }

        return productNames;
    }

    /**
     * 判断是否为产品名称文本 - 严格条件
     */
    private boolean isProductNameText(OcrTextResult text) {
        String content = text.getText().trim();

        // 长度检查
        if (content.length() < LayoutConfig.MIN_PRODUCT_NAME_LENGTH ||
                content.length() > LayoutConfig.MAX_PRODUCT_NAME_LENGTH) {
            return false;
        }

        // 排除纯数字和金额格式
        if (textAnalysisUtil.isAmountFormat(content) || content.matches("^[0-9,./\\s]+$")) {
            return false;
        }

        // 排除常见的元数据文本
        if (isMetadataText(content)) {
            return false;
        }

        // 产品名称特征检查 - 更宽松的条件
        boolean hasProductKeywords = content.contains("理财") || content.contains("基金") ||
                content.contains("债券") || content.contains("精选") ||
                content.contains("恒盈") || content.contains("代销") ||
                content.contains("工银") || content.contains("交银") ||
                content.contains("兴银") || content.contains("平安") ||
                content.contains("优选") || content.contains("稳利") ||
                content.contains("创利") || content.contains("灵活");

        boolean hasStructuralChars = content.contains("|") || content.contains("·") ||
                content.contains("（") || content.contains("）");

        boolean hasReasonableLength = content.length() >= 15; // 降低长度要求

        return hasProductKeywords || (hasStructuralChars && hasReasonableLength);
    }

    /**
     * 判断是否为产品名称文本 - 宽松条件
     */
    private boolean isProductNameTextLoose(OcrTextResult text) {
        String content = text.getText().trim();

        // 最基本的检查
        if (content.length() < 8 || content.length() > 100) {
            return false;
        }

        // 排除纯数字、日期和金额格式
        if (textAnalysisUtil.isAmountFormat(content) ||
                content.matches("^[0-9,./\\s]+$") ||
                content.matches("^\\d{4}/\\d{2}/\\d{2}$")) {
            return false;
        }

        // 排除明显的元数据
        if (isMetadataText(content)) {
            return false;
        }

        // 宽松的产品名称特征
        boolean hasProductIndicators = content.contains("理财") || content.contains("基金") ||
                content.contains("债券") || content.contains("精选") ||
                content.contains("恒盈") || content.contains("代销") ||
                content.contains("稳利") || content.contains("灵活") ||
                content.contains("优选") || content.contains("创利");

        boolean hasInstitutionNames = content.contains("工银") || content.contains("交银") ||
                content.contains("兴银") || content.contains("平安") ||
                content.contains("招商") || content.contains("中信");

        boolean hasStructure = content.contains("|") || content.contains("·") ||
                content.contains("号") || content.contains("款");

        // 长文本且包含中文的，很可能是产品名称
        boolean isLongChineseText = content.length() >= 12 &&
                content.matches(".*[\\u4e00-\\u9fa5].*") &&
                !content.matches(".*[今日|每日|可赎|持仓|市值].*");

        return hasProductIndicators || hasInstitutionNames || hasStructure || isLongChineseText;
    }

    /**
     * 基于产品名称创建垂直组
     */
    private List<VerticalGroup> createGroupsAroundProducts(PageLayout layout, List<OcrTextResult> productNames) {
        List<VerticalGroup> groups = new ArrayList<>();

        for (OcrTextResult productName : productNames) {
            VerticalGroup group = createGroupAroundProduct(layout.getValidTexts(), productName);
            if (group.getTexts().size() >= LayoutConfig.MIN_VERTICAL_GROUP_SIZE) {
                groups.add(group);
                log.debug("创建产品组: '{}' (文本数: {})",
                        productName.getText(), group.getTexts().size());
            }
        }

        // 按垂直位置排序
        groups.sort(Comparator.comparingDouble(VerticalGroup::getTopY));

        return groups;
    }

    /**
     * 围绕产品名称创建组
     */
    private VerticalGroup createGroupAroundProduct(List<OcrTextResult> allTexts, OcrTextResult productText) {
        VerticalGroup group = new VerticalGroup();
        group.setCenterX(productText.getBbox().getCenterX());
        group.getTexts().add(productText);

        double productY = productText.getBbox().getCenterY();

        // 在产品名称下方寻找相关文本
        List<OcrTextResult> candidatesBelow = findTextsBelow(allTexts, productText, LayoutConfig.PRODUCT_SEARCH_RANGE);

        // 优先寻找金额文本
        List<OcrTextResult> amountTexts = candidatesBelow.stream()
                .filter(text -> textAnalysisUtil.isAmountFormat(text.getText().trim()))
                .sorted(Comparator.comparingDouble(t -> t.getBbox().getCenterY()))
                .collect(Collectors.toList());

        if (!amountTexts.isEmpty()) {
            // 添加第一个（最近的）金额文本
            OcrTextResult primaryAmount = amountTexts.get(0);
            group.getTexts().add(primaryAmount);
            log.debug("为产品 '{}' 找到金额: {}", productText.getText(), primaryAmount.getText());

            // 在金额文本附近寻找其他相关文本（但排除元数据）
            double amountY = primaryAmount.getBbox().getCenterY();
            List<OcrTextResult> nearbyTexts = candidatesBelow.stream()
                    .filter(text -> !text.equals(primaryAmount))
                    .filter(text -> Math.abs(text.getBbox().getCenterY() - amountY) <= LayoutConfig.METADATA_DISTANCE_THRESHOLD)
                    .filter(text -> !isCommonMetadata(text.getText().trim()))
                    .limit(3) // 限制数量
                    .collect(Collectors.toList());

            group.getTexts().addAll(nearbyTexts);
        } else {
            log.debug("产品 '{}' 下方未找到金额文本", productText.getText());
            // 如果没有找到金额，尝试在更大范围内寻找
            List<OcrTextResult> allCandidates = findTextsBelow(allTexts, productText, 300.0);
            Optional<OcrTextResult> distantAmount = allCandidates.stream()
                    .filter(text -> textAnalysisUtil.isAmountFormat(text.getText().trim()))
                    .findFirst();

            if (distantAmount.isPresent()) {
                group.getTexts().add(distantAmount.get());
                log.debug("为产品 '{}' 找到远距离金额: {}", productText.getText(), distantAmount.get().getText());
            }
        }

        // 按Y坐标排序
        group.getTexts().sort(Comparator.comparingDouble(t -> t.getBbox().getCenterY()));

        if (!group.getTexts().isEmpty()) {
            group.setTopY(group.getTexts().get(0).getBbox().getTop());
            group.setBottomY(group.getTexts().get(group.getTexts().size() - 1).getBbox().getBottom());
        }

        return group;
    }

    /**
     * 寻找指定文本下方的文本
     */
    private List<OcrTextResult> findTextsBelow(List<OcrTextResult> allTexts, OcrTextResult referenceText, double searchRange) {
        double refY = referenceText.getBbox().getCenterY();

        return allTexts.stream()
                .filter(text -> !text.equals(referenceText))
                .filter(text -> {
                    double textY = text.getBbox().getCenterY();
                    return textY > refY && textY <= refY + searchRange;
                })
                .sorted(Comparator.comparingDouble(t -> t.getBbox().getCenterY()))
                .collect(Collectors.toList());
    }

    /**
     * 从垂直组构建产品项
     */
    private ProductItem buildProductFromVerticalGroup(VerticalGroup group, int groupIndex) {
        log.debug("开始构建垂直组 {} 的产品项，文本数量: {}", groupIndex, group.getTexts().size());

        if (group.getTexts().isEmpty()) {
            return null;
        }

        // 分析组内文本类型
        ProductTextAnalysis analysis = analyzeTextsInGroup(group);

        if (analysis.getAmountTexts().isEmpty()) {
            log.debug("垂直组 {} 未找到金额文本", groupIndex);
            return null;
        }

        // 构建产品项
        return buildProductFromAnalysis(analysis, groupIndex);
    }

    /**
     * 分析组内文本类型
     */
    private ProductTextAnalysis analyzeTextsInGroup(VerticalGroup group) {
        ProductTextAnalysis analysis = new ProductTextAnalysis();

        for (OcrTextResult text : group.getTexts()) {
            String content = text.getText().trim();

            if (textAnalysisUtil.isAmountFormat(content)) {
                BigDecimal amount = textAnalysisUtil.parseAmount(content);
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    analysis.getAmountTexts().add(text);
                    log.debug("识别到金额: {} -> {}", content, amount);
                }
            } else if (isProductNameTextLoose(text)) {
                analysis.getProductNameTexts().add(text);
                log.debug("识别到产品名称: {}", content);
            } else if (isMetadataText(content)) {
                analysis.getMetadataTexts().add(text);
                log.debug("识别到元数据: {}", content);
            } else if (textAnalysisUtil.isMeaningfulContent(content)) {
                analysis.getOtherTexts().add(text);
                log.debug("识别到其他内容: {}", content);
            }
        }

        return analysis;
    }

    /**
     * 判断是否为元数据文本
     */
    private boolean isMetadataText(String content) {
        Set<String> metadataKeywords = Set.of(
                "持仓市值", "可赎回日", "赎回类型", "今日可赎", "每日可赎",
                "预约赎回", "周期结束日", "可赎回开始日", "产品持仓", "撤单",
                "最短持有期内不可赎回，可赎回开始日起每日可赎"
        );

        return metadataKeywords.contains(content.trim());
    }

    /**
     * 从分析结果构建产品
     */
    private ProductItem buildProductFromAnalysis(ProductTextAnalysis analysis, int groupIndex) {
        ProductItem product = new ProductItem();

        // 选择主要金额
        OcrTextResult primaryAmount = selectPrimaryAmount(analysis.getAmountTexts());
        BigDecimal amount = textAnalysisUtil.parseAmount(primaryAmount.getText().trim());
        product.setAmountText(primaryAmount);
        product.setAmount(amount);

        // 构建产品名称
        String productName = extractProductName(analysis);
        product.setOriginalName(productName);
        product.setCleanName(textAnalysisUtil.cleanProductName(productName));

        // 设置相关文本（排除金额文本和常见元数据）
        List<OcrTextResult> relatedTexts = new ArrayList<>();
        relatedTexts.addAll(analysis.getProductNameTexts());
        relatedTexts.addAll(analysis.getOtherTexts());
        // 只添加有意义的元数据
        analysis.getMetadataTexts().stream()
                .filter(text -> !isCommonMetadata(text.getText()))
                .forEach(relatedTexts::add);

        product.setRelatedTexts(relatedTexts);

        // 计算置信度
        List<OcrTextResult> allTexts = new ArrayList<>(relatedTexts);
        allTexts.add(primaryAmount);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));

        log.debug("构建产品完成: 原始='{}', 清理='{}', 金额={}, 置信度={}",
                productName, product.getCleanName(), amount, product.getConfidence());

        return product;
    }

    /**
     * 选择主要金额文本
     */
    private OcrTextResult selectPrimaryAmount(List<OcrTextResult> amountTexts) {
        if (amountTexts.size() == 1) {
            return amountTexts.get(0);
        }

        // 优先选择格式完整的金额（包含逗号分隔符且金额合理）
        Optional<OcrTextResult> preferredAmount = amountTexts.stream()
                .filter(text -> {
                    String content = text.getText().trim();
                    BigDecimal amount = textAnalysisUtil.parseAmount(content);
                    return content.contains(",") &&
                            amount != null &&
                            amount.compareTo(new BigDecimal("0.01")) >= 0 &&
                            amount.compareTo(new BigDecimal("999999999")) <= 0;
                })
                .findFirst();

        if (preferredAmount.isPresent()) {
            return preferredAmount.get();
        }

        // 如果没有首选的，选择第一个有效金额
        return amountTexts.stream()
                .filter(text -> {
                    BigDecimal amount = textAnalysisUtil.parseAmount(text.getText().trim());
                    return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
                })
                .findFirst()
                .orElse(amountTexts.get(0));
    }

    /**
     * 提取产品名称
     */
    private String extractProductName(ProductTextAnalysis analysis) {
        // 优先使用识别的产品名称文本
        if (!analysis.getProductNameTexts().isEmpty()) {
            return analysis.getProductNameTexts().get(0).getText().trim();
        }

        // 尝试从其他文本构建产品名称
        String fallbackName = analysis.getOtherTexts().stream()
                .filter(text -> text.getText().length() > 5)
                .filter(text -> !isCommonMetadata(text.getText()))
                .map(OcrTextResult::getText)
                .collect(Collectors.joining(""));

        return StringUtils.isNotBlank(fallbackName) ? fallbackName : "未知产品";
    }

    /**
     * 判断是否为常见元数据
     */
    private boolean isCommonMetadata(String content) {
        Set<String> commonMetadata = Set.of(
                "持仓市值", "今日可赎", "每日可赎", "可赎回日", "赎回类型"
        );
        return commonMetadata.contains(content.trim());
    }

    /**
     * 传统分组方法（回退方案）- 改进版
     */
    private List<ProductItem> processWithTraditionalGrouping(PageLayout layout) {
        log.debug("使用改进的传统分组方法");

        // 1. 先尝试通过金额来反向寻找产品名称
        List<OcrTextResult> amountTexts = layout.getValidTexts().stream()
                .filter(text -> textAnalysisUtil.isAmountFormat(text.getText().trim()))
                .filter(text -> {
                    BigDecimal amount = textAnalysisUtil.parseAmount(text.getText().trim());
                    return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.toList());

        List<ProductItem> products = new ArrayList<>();
        Set<OcrTextResult> usedTexts = new HashSet<>();

        for (OcrTextResult amountText : amountTexts) {
            if (usedTexts.contains(amountText)) {
                continue;
            }

            // 在金额上方寻找产品名称
            List<OcrTextResult> textsAbove = findTextsAbove(layout.getValidTexts(), amountText, 200.0);

            // 寻找最可能的产品名称
            Optional<OcrTextResult> productName = textsAbove.stream()
                    .filter(text -> !usedTexts.contains(text))
                    .filter(text -> !isMetadataText(text.getText().trim()))
                    .filter(text -> isProductNameTextLoose(text))
                    .findFirst();

            if (productName.isPresent()) {
                ProductItem product = buildSimpleProduct(productName.get(), amountText);
                if (product != null) {
                    products.add(product);
                    usedTexts.add(amountText);
                    usedTexts.add(productName.get());
                    log.debug("传统方法构建产品: '{}' - {}",
                            product.getCleanName(), product.getAmount());
                }
            } else {
                // 如果没有找到明确的产品名称，使用附近的文本
                List<OcrTextResult> nearbyTexts = findNearbyTexts(layout.getValidTexts(), amountText, 100.0);
                if (!nearbyTexts.isEmpty()) {
                    ProductItem product = buildProductFromNearbyTexts(nearbyTexts, amountText);
                    if (product != null) {
                        products.add(product);
                        usedTexts.add(amountText);
                        usedTexts.addAll(nearbyTexts);
                    }
                }
            }
        }

        log.debug("传统分组方法完成，产品数量: {}", products.size());
        return products;
    }

    /**
     * 寻找指定文本上方的文本
     */
    private List<OcrTextResult> findTextsAbove(List<OcrTextResult> allTexts, OcrTextResult referenceText, double searchRange) {
        double refY = referenceText.getBbox().getCenterY();

        return allTexts.stream()
                .filter(text -> !text.equals(referenceText))
                .filter(text -> {
                    double textY = text.getBbox().getCenterY();
                    return textY < refY && textY >= refY - searchRange;
                })
                .sorted(Comparator.comparingDouble(t -> -t.getBbox().getCenterY())) // 按Y坐标倒序
                .collect(Collectors.toList());
    }

    /**
     * 寻找附近的文本
     */
    private List<OcrTextResult> findNearbyTexts(List<OcrTextResult> allTexts, OcrTextResult referenceText, double searchRange) {
        double refY = referenceText.getBbox().getCenterY();

        return allTexts.stream()
                .filter(text -> !text.equals(referenceText))
                .filter(text -> {
                    double textY = text.getBbox().getCenterY();
                    return Math.abs(textY - refY) <= searchRange;
                })
                .filter(text -> !isMetadataText(text.getText().trim()))
                .filter(text -> !textAnalysisUtil.isAmountFormat(text.getText().trim()))
                .collect(Collectors.toList());
    }

    /**
     * 构建简单产品项
     */
    private ProductItem buildSimpleProduct(OcrTextResult productNameText, OcrTextResult amountText) {
        ProductItem product = new ProductItem();

        BigDecimal amount = textAnalysisUtil.parseAmount(amountText.getText().trim());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        product.setAmountText(amountText);
        product.setAmount(amount);

        String originalName = productNameText.getText().trim();
        product.setOriginalName(originalName);
        product.setCleanName(textAnalysisUtil.cleanProductName(originalName));

        List<OcrTextResult> relatedTexts = Arrays.asList(productNameText);
        product.setRelatedTexts(relatedTexts);

        List<OcrTextResult> allTexts = Arrays.asList(productNameText, amountText);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));

        return product;
    }

    /**
     * 从附近文本构建产品
     */
    private ProductItem buildProductFromNearbyTexts(List<OcrTextResult> nearbyTexts, OcrTextResult amountText) {
        if (nearbyTexts.isEmpty()) {
            return null;
        }

        ProductItem product = new ProductItem();

        BigDecimal amount = textAnalysisUtil.parseAmount(amountText.getText().trim());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        product.setAmountText(amountText);
        product.setAmount(amount);

        String originalName = nearbyTexts.stream()
                .map(OcrTextResult::getText)
                .collect(Collectors.joining(""));

        product.setOriginalName(originalName);
        product.setCleanName(textAnalysisUtil.cleanProductName(originalName));
        product.setRelatedTexts(nearbyTexts);

        List<OcrTextResult> allTexts = new ArrayList<>(nearbyTexts);
        allTexts.add(amountText);
        product.setConfidence(textAnalysisUtil.calculateAverageConfidence(allTexts));

        return product;
    }

    /**
     * 产品文本分析结果
     */
    private static class ProductTextAnalysis {
        private final List<OcrTextResult> productNameTexts = new ArrayList<>();
        private final List<OcrTextResult> amountTexts = new ArrayList<>();
        private final List<OcrTextResult> metadataTexts = new ArrayList<>();
        private final List<OcrTextResult> otherTexts = new ArrayList<>();

        public List<OcrTextResult> getProductNameTexts() { return productNameTexts; }
        public List<OcrTextResult> getAmountTexts() { return amountTexts; }
        public List<OcrTextResult> getMetadataTexts() { return metadataTexts; }
        public List<OcrTextResult> getOtherTexts() { return otherTexts; }
    }

    /**
     * 垂直组类
     */
    public static class VerticalGroup {
        private double centerX;
        private double topY;
        private double bottomY;
        private final List<OcrTextResult> texts = new ArrayList<>();

        public double getCenterX() { return centerX; }
        public void setCenterX(double centerX) { this.centerX = centerX; }
        public double getTopY() { return topY; }
        public void setTopY(double topY) { this.topY = topY; }
        public double getBottomY() { return bottomY; }
        public void setBottomY(double bottomY) { this.bottomY = bottomY; }
        public List<OcrTextResult> getTexts() { return texts; }
    }
}