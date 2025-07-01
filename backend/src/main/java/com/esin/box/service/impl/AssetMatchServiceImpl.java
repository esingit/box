package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.dto.ocr.*;
import com.esin.box.entity.AssetName;
import com.esin.box.service.AssetMatchService;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.processor.ColumnLayoutProcessor;
import com.esin.box.service.processor.LayoutProcessor;
import com.esin.box.service.processor.RowLayoutProcessor;
import com.esin.box.utils.TextAnalysisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支持多布局的资产匹配服务主实现类（完全优化版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetMatchServiceImpl implements AssetMatchService {

    private final AssetNameService assetNameService;
    private final ColumnLayoutProcessor columnLayoutProcessor;
    private final RowLayoutProcessor rowLayoutProcessor;
    private final TextAnalysisUtil textAnalysisUtil;
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();

    @Value("${app.business.ocr.asset-match.confirm-threshold:0.7}")
    private double confirmThreshold;

    @Value("${app.business.ocr.asset-match.min-threshold:0.3}")
    private double minThreshold;

    @Value("${app.business.ocr.asset-match.max-results:20}")
    private int maxResults;

    @Override
    public List<AssetScanImageDTO> matchAssetsFromOcr(List<OcrTextResult> ocrTexts, String username) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("=== 开始多布局资产匹配 ===");
        log.info("用户: {}, OCR文本数量: {}", username, ocrTexts.size());

        try {
            // 1. 获取用户资产
            List<AssetName> userAssets = getUserAssets(username);
            if (userAssets.isEmpty()) {
                log.warn("用户没有资产数据: {}", username);
                return Collections.emptyList();
            }

            // 2. 分析页面基础信息
            PageLayout pageLayout = analyzePageLayout(ocrTexts);

            // 3. 智能布局检测（完全重写）
            LayoutType layoutType = detectLayoutTypeIntelligent(pageLayout);
            log.info("检测到布局类型: {}", layoutType);

            // 4. 根据布局类型选择处理器
            LayoutProcessor processor = selectProcessor(layoutType);

            // 5. 处理文本并构建产品项
            List<ProductItem> products = processor.processLayout(pageLayout);
            log.info("处理得到产品项: {}", products.size());

            // 6. 匹配资产
            List<AssetScanImageDTO> results = matchProducts(products, userAssets);
            log.info("匹配结果: {}", results.size());

            return results;

        } catch (Exception e) {
            log.error("资产匹配过程出现异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 分析页面布局
     */
    private PageLayout analyzePageLayout(List<OcrTextResult> ocrTexts) {
        PageLayout layout = new PageLayout();

        // 过滤有效文本
        layout.setValidTexts(ocrTexts.stream()
                .filter(textAnalysisUtil::isValidText)
                .sorted(Comparator.comparingDouble(text -> text.getBbox().getCenterY()))
                .collect(Collectors.toList()));

        if (layout.getValidTexts().isEmpty()) {
            return layout;
        }

        // 计算页面尺寸
        double width = layout.getValidTexts().stream()
                .mapToDouble(text -> text.getBbox().getRight())
                .max().orElse(0);

        double height = layout.getValidTexts().stream()
                .mapToDouble(text -> text.getBbox().getBottom())
                .max().orElse(0);

        layout.setWidth(width);
        layout.setHeight(height);
        layout.setLeftColumnBoundary(width * 0.4);
        layout.setRightColumnBoundary(width * 0.6);

        log.debug("页面布局分析完成: 宽度={}, 高度={}, 有效文本={}",
                width, height, layout.getValidTexts().size());

        return layout;
    }

    /**
     * 智能布局检测 - 完全重写
     */
    private LayoutType detectLayoutTypeIntelligent(PageLayout layout) {
        if (layout.getValidTexts().size() < 4) {
            return LayoutType.UNKNOWN;
        }

        log.debug("=== 开始智能布局检测 ===");

        // 1. 优先检测产品名称驱动的上下行布局
        boolean isProductNameRowLayout = detectProductNameRowLayout(layout);
        if (isProductNameRowLayout) {
            log.info("检测为产品名称驱动的上下行布局");
            return LayoutType.TOP_BOTTOM_ROWS;
        }

        // 2. 检测传统的左右列布局
        boolean isColumnLayout = detectTraditionalColumnLayout(layout);
        if (isColumnLayout) {
            log.info("检测为传统左右列布局");
            return LayoutType.LEFT_RIGHT_COLUMNS;
        }

        // 3. 检测基于行的上下布局
        boolean isRowLayout = detectRowBasedLayout(layout);
        if (isRowLayout) {
            log.info("检测为基于行的上下布局");
            return LayoutType.TOP_BOTTOM_ROWS;
        }

        // 4. 默认使用上下行布局（因为这种布局更常见）
        log.info("无法明确识别布局类型，默认使用上下行布局");
        return LayoutType.TOP_BOTTOM_ROWS;
    }

    /**
     * 检测产品名称驱动的上下行布局
     */
    private boolean detectProductNameRowLayout(PageLayout layout) {
        log.debug("检测产品名称驱动的上下行布局");

        // 1. 识别产品名称文本 - 使用智能双重策略
        List<OcrTextResult> productNames = identifyProductNamesIntelligent(layout.getValidTexts());
        log.debug("识别到产品名称数量: {}", productNames.size());

        // 输出识别到的产品名称
        for (OcrTextResult productName : productNames) {
            log.debug("识别到产品名称: '{}'", productName.getText());
        }

        if (productNames.size() < 2) {
            log.debug("产品名称数量不足，不是产品名称驱动布局");
            return false;
        }

        // 2. 检查每个产品名称下方是否有对应的金额
        int validProductPairs = 0;
        for (OcrTextResult productName : productNames) {
            List<OcrTextResult> amountsBelow = findAmountsBelow(layout.getValidTexts(), productName, 200.0);
            if (!amountsBelow.isEmpty()) {
                validProductPairs++;
                log.debug("产品 '{}' 下方找到 {} 个金额",
                        productName.getText(), amountsBelow.size());
            }
        }

        double validRatio = (double) validProductPairs / productNames.size();
        log.debug("有效产品-金额对比例: {}/{} = {}", validProductPairs, productNames.size(), validRatio);

        boolean isProductRowLayout = validRatio >= 0.6; // 60%以上的产品名称有对应金额
        log.debug("产品名称驱动布局检测结果: {}", isProductRowLayout);

        return isProductRowLayout;
    }

    /**
     * 智能识别产品名称文本 - 使用双重策略
     */
    private List<OcrTextResult> identifyProductNamesIntelligent(List<OcrTextResult> texts) {
        // 首先尝试严格条件
        List<OcrTextResult> productNames = texts.stream()
                .filter(this::isProductNameTextStrict)
                .collect(Collectors.toList());

        log.debug("严格条件识别到产品名称数量: {}", productNames.size());
        for (OcrTextResult text : productNames) {
            log.debug("  - 严格条件产品名称: '{}'", text.getText());
        }

        // 如果严格条件没找到，尝试宽松条件
        if (productNames.isEmpty()) {
            log.debug("严格条件未找到产品名称，尝试宽松条件");
            productNames = texts.stream()
                    .filter(this::isProductNameTextLoose)
                    .collect(Collectors.toList());
            log.debug("宽松条件识别到产品名称数量: {}", productNames.size());
            for (OcrTextResult text : productNames) {
                log.debug("  - 宽松条件产品名称: '{}'", text.getText());
            }
        }

        return productNames;
    }

    /**
     * 判断是否为产品名称文本 - 严格条件 - 修复版本
     */
    private boolean isProductNameTextStrict(OcrTextResult text) {
        String content = text.getText().trim();

        log.debug("检查产品名称 (严格): '{}'", content);

        // 长度检查
        if (content.length() < 8 || content.length() > 100) {
            log.debug("  - 长度不符合: {} (要求: 8-100)", content.length());
            return false;
        }

        // 使用更精确的金额格式判断
        if (isStrictAmountFormat(content)) {
            log.debug("  - 是金额格式");
            return false;
        }

        if (content.matches("^[0-9,./\\s]+$")) {
            log.debug("  - 是纯数字");
            return false;
        }

        // 排除日期格式
        if (content.matches("^\\d{4}/\\d{2}/\\d{2}$")) {
            log.debug("  - 是日期格式");
            return false;
        }

        // 排除常见的元数据文本
        if (isMetadataTextInAssetMatch(content)) {
            log.debug("  - 是元数据文本");
            return false;
        }

        // 产品名称特征检查
        boolean hasProductKeywords = content.contains("理财") || content.contains("基金") ||
                content.contains("债券") || content.contains("精选") ||
                content.contains("恒盈") || content.contains("代销") ||
                content.contains("工银") || content.contains("交银") ||
                content.contains("兴银") || content.contains("平安") ||
                content.contains("优选") || content.contains("稳利") ||
                content.contains("创利") || content.contains("灵活");

        boolean hasStructuralChars = content.contains("|") || content.contains("·") ||
                content.contains("（") || content.contains("）");

        boolean hasReasonableLength = content.length() >= 12;

        boolean result = hasProductKeywords || (hasStructuralChars && hasReasonableLength);

        log.debug("  - 产品关键词: {}, 结构字符: {}, 合理长度: {}, 结果: {}",
                hasProductKeywords, hasStructuralChars, hasReasonableLength, result);

        if (result) {
            log.debug("  - ✓ 严格条件识别为产品名称: '{}'", content);
        }

        return result;
    }

    /**
     * 判断是否为产品名称文本 - 宽松条件 - 修复版本
     */
    private boolean isProductNameTextLoose(OcrTextResult text) {
        String content = text.getText().trim();

        log.debug("检查产品名称 (宽松): '{}'", content);

        // 最基本的检查
        if (content.length() < 8 || content.length() > 100) {
            log.debug("  - 长度不符合: {} (要求: 8-100)", content.length());
            return false;
        }

        // 使用更精确的金额格式判断
        if (isStrictAmountFormat(content)) {
            log.debug("  - 是金额格式");
            return false;
        }

        if (content.matches("^[0-9,./\\s]+$")) {
            log.debug("  - 是纯数字");
            return false;
        }

        if (content.matches("^\\d{4}/\\d{2}/\\d{2}$")) {
            log.debug("  - 是日期格式");
            return false;
        }

        // 排除明显的元数据
        if (isMetadataTextInAssetMatch(content)) {
            log.debug("  - 是元数据文本");
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
        boolean isLongChineseText = content.length() >= 10 &&
                content.matches(".*[\\u4e00-\\u9fa5].*") &&
                !content.matches(".*(今日|每日|可赎|持仓|市值).*");

        boolean result = hasProductIndicators || hasInstitutionNames || hasStructure || isLongChineseText;

        log.debug("  - 产品指标: {}, 机构名称: {}, 结构: {}, 长中文: {}, 结果: {}",
                hasProductIndicators, hasInstitutionNames, hasStructure, isLongChineseText, result);

        if (result) {
            log.debug("  - ✓ 宽松条件识别为产品名称: '{}'", content);
        }

        return result;
    }

    /**
     * 更精确的金额格式判断
     */
    private boolean isStrictAmountFormat(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 严格的金额格式模式
        // 1. 纯数字金额：123.45, 1,234.56, 123,456.78
        if (trimmed.matches("^\\d{1,3}(,\\d{3})*(\\.\\d{2})?$")) {
            return true;
        }

        // 2. 简单小数：12.34, 123.4, 1234.56
        if (trimmed.matches("^\\d+\\.\\d{1,4}$")) {
            return true;
        }

        // 3. 整数金额：123, 1234, 12345
        if (trimmed.matches("^\\d{1,10}$")) {
            return true;
        }

        // 4. 日期格式（明确排除）
        if (trimmed.matches("^\\d{4}/\\d{2}/\\d{2}$")) {
            return false;
        }

        // 5. 包含中文的不是金额
        if (trimmed.matches(".*[\\u4e00-\\u9fa5].*")) {
            return false;
        }

        // 6. 包含字母的不是金额
        if (trimmed.matches(".*[a-zA-Z].*")) {
            return false;
        }

        // 7. 包含特殊符号的（除了逗号和点）不是金额
        if (trimmed.matches(".*[^0-9,.].*")) {
            return false;
        }

        return false;
    }

    /**
     * 寻找指定文本下方的金额 - 使用严格金额判断
     */
    private List<OcrTextResult> findAmountsBelow(List<OcrTextResult> allTexts, OcrTextResult referenceText, double searchRange) {
        double refY = referenceText.getBbox().getCenterY();

        return allTexts.stream()
                .filter(text -> !text.equals(referenceText))
                .filter(text -> {
                    double textY = text.getBbox().getCenterY();
                    return textY > refY && textY <= refY + searchRange;
                })
                .filter(text -> isStrictAmountFormat(text.getText().trim()))
                .collect(Collectors.toList());
    }

    /**
     * 检测传统的左右列布局 - 使用严格金额判断
     */
    private boolean detectTraditionalColumnLayout(PageLayout layout) {
        log.debug("检测传统左右列布局");

        // 只有在明确满足左右列特征时才判断为列布局
        // 1. 金额文本高度集中在页面右侧
        List<OcrTextResult> amountTexts = layout.getValidTexts().stream()
                .filter(text -> isStrictAmountFormat(text.getText().trim()))
                .collect(Collectors.toList());

        if (amountTexts.size() < 3) {
            log.debug("金额文本数量不足: {}", amountTexts.size());
            return false;
        }

        // 2. 检查金额是否主要分布在页面右侧
        double pageWidth = layout.getWidth();
        double rightBoundary = pageWidth * 0.6; // 右侧60%区域

        long rightSideAmounts = amountTexts.stream()
                .filter(text -> text.getBbox().getLeft() > rightBoundary)
                .count();

        double rightSideRatio = (double) rightSideAmounts / amountTexts.size();
        log.debug("金额右侧分布比例: {}/{} = {}", rightSideAmounts, amountTexts.size(), rightSideRatio);

        // 3. 检查是否有明显的左右列分布
        boolean hasColumnStructure = hasObviousColumnStructure(layout);

        boolean isColumnLayout = rightSideRatio >= 0.7 && hasColumnStructure;
        log.debug("传统左右列布局检测结果: {}", isColumnLayout);

        return isColumnLayout;
    }

    /**
     * 判断是否为元数据文本 - AssetMatchServiceImpl版本
     */
    private boolean isMetadataTextInAssetMatch(String content) {
        Set<String> metadataKeywords = Set.of(
                "持仓市值", "可赎回日", "赎回类型", "今日可赎", "每日可赎",
                "预约赎回", "周期结束日", "可赎回开始日", "产品持仓", "撤单",
                "最短持有期内不可赎回，可赎回开始日起每日可赎"
        );

        return metadataKeywords.contains(content.trim());
    }

    /**
     * 检查是否有明显的列结构
     */
    private boolean hasObviousColumnStructure(PageLayout layout) {
        // X坐标聚类分析
        List<Double> xPositions = layout.getValidTexts().stream()
                .mapToDouble(text -> text.getBbox().getLeft())
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        List<ColumnCluster> clusters = clusterXPositions(xPositions, layout.getWidth());

        if (clusters.size() < 2) {
            return false;
        }

        // 检查是否有明显的间距
        ColumnCluster leftCluster = clusters.get(0);
        ColumnCluster rightCluster = clusters.get(clusters.size() - 1);

        double gap = rightCluster.getCenter() - leftCluster.getCenter();
        double gapRatio = gap / layout.getWidth();

        return gapRatio > 0.3 && leftCluster.getCount() >= 3 && rightCluster.getCount() >= 3;
    }

    /**
     * 检测基于行的上下布局
     */
    private boolean detectRowBasedLayout(PageLayout layout) {
        log.debug("检测基于行的上下布局");

        // 按Y坐标分组，寻找水平排列的文本行
        Map<Integer, List<OcrTextResult>> rowGroups = groupTextsByRow(layout.getValidTexts());

        // 检查是否有多个包含多个元素的行
        long multiElementRows = rowGroups.values().stream()
                .filter(row -> row.size() >= 2)
                .count();

        boolean isRowLayout = multiElementRows >= 3; // 至少3行有多个元素
        log.debug("基于行的上下布局检测结果: {} (多元素行数: {})", isRowLayout, multiElementRows);

        return isRowLayout;
    }

    /**
     * 选择处理器
     */
    private LayoutProcessor selectProcessor(LayoutType layoutType) {
        return switch (layoutType) {
            case TOP_BOTTOM_ROWS -> {
                log.info("选择上下行处理器");
                yield rowLayoutProcessor;
            }
            case LEFT_RIGHT_COLUMNS -> {
                log.info("选择左右列处理器");
                yield columnLayoutProcessor;
            }
            case MIXED_LAYOUT -> {
                log.info("选择上下行处理器（混合布局）");
                yield rowLayoutProcessor; // 混合布局优先使用行处理器
            }
            default -> {
                log.info("使用默认上下行处理器");
                yield rowLayoutProcessor; // 默认使用行处理器
            }
        };
    }

    /**
     * X坐标聚类
     */
    private List<ColumnCluster> clusterXPositions(List<Double> positions, double pageWidth) {
        List<ColumnCluster> clusters = new ArrayList<>();
        if (positions.isEmpty()) return clusters;

        double tolerance = Math.max(pageWidth * 0.08, 30.0);
        ColumnCluster currentCluster = new ColumnCluster(positions.get(0));

        for (int i = 1; i < positions.size(); i++) {
            double pos = positions.get(i);

            if (Math.abs(pos - currentCluster.getCenter()) <= tolerance) {
                currentCluster.add(pos);
            } else {
                if (currentCluster.getCount() >= 2) {
                    clusters.add(currentCluster);
                }
                currentCluster = new ColumnCluster(pos);
            }
        }

        if (currentCluster.getCount() >= 2) {
            clusters.add(currentCluster);
        }

        return clusters;
    }

    private Map<Integer, List<OcrTextResult>> groupTextsByRow(List<OcrTextResult> texts) {
        Map<Integer, List<OcrTextResult>> groups = new TreeMap<>();

        for (OcrTextResult text : texts) {
            int rowKey = (int) (text.getBbox().getCenterY() / 30.0);
            groups.computeIfAbsent(rowKey, k -> new ArrayList<>()).add(text);
        }

        return groups;
    }

    /**
     * 匹配产品到资产 - 保持页面顺序
     */
    private List<AssetScanImageDTO> matchProducts(List<ProductItem> products, List<AssetName> userAssets) {
        List<AssetScanImageDTO> results = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            ProductItem product = products.get(i);
            AssetMatchResult matchResult = findBestMatch(product, userAssets);

            AssetScanImageDTO dto = buildResultDTO(product, matchResult);
            results.add(dto);

            String displayAmount = textAnalysisUtil.formatAmountDisplay(product.getAmount());

            if (matchResult != null) {
                log.info("产品 {} 匹配: '{}' -> '{}' (分数: {}%, 金额: {})",
                        i + 1, product.getCleanName(), matchResult.getAssetName().getName(),
                        Math.round(matchResult.getScore() * 100), displayAmount);
            } else {
                log.warn("产品 {} 无匹配: '{}' (金额: {})",
                        i + 1, product.getCleanName(), displayAmount);
            }
        }

        return results.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 查找最佳匹配
     */
    private AssetMatchResult findBestMatch(ProductItem product, List<AssetName> userAssets) {
        double bestScore = 0.0;
        AssetName bestAsset = null;

        for (AssetName asset : userAssets) {
            double score = calculateSimilarity(product.getCleanName(), asset.getName());

            if (score >= minThreshold && score > bestScore) {
                bestScore = score;
                bestAsset = asset;
            }
        }

        if (bestAsset == null) {
            return null;
        }

        AssetMatchResult result = new AssetMatchResult();
        result.setAssetName(bestAsset);
        result.setScore(bestScore);
        return result;
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        if (StringUtils.isBlank(text1) || StringUtils.isBlank(text2)) {
            return 0.0;
        }

        String clean1 = textAnalysisUtil.cleanProductName(text1).toLowerCase();
        String clean2 = textAnalysisUtil.cleanProductName(text2).toLowerCase();

        // 完全匹配
        if (clean1.equals(clean2)) {
            return 1.0;
        }

        // 包含匹配
        if (clean1.contains(clean2) || clean2.contains(clean1)) {
            double ratio = (double) Math.min(clean1.length(), clean2.length()) /
                    Math.max(clean1.length(), clean2.length());
            return 0.8 + 0.15 * ratio;
        }

        // Jaro-Winkler 相似度
        double jwSimilarity = jaroWinklerSimilarity.apply(clean1, clean2);

        // 字符集合相似度
        double charSimilarity = calculateCharSimilarity(clean1, clean2);

        return Math.max(jwSimilarity, charSimilarity);
    }

    /**
     * 计算字符相似度
     */
    private double calculateCharSimilarity(String text1, String text2) {
        Set<Character> chars1 = text1.chars()
                .mapToObj(ch -> (char) ch)
                .collect(Collectors.toSet());

        Set<Character> chars2 = text2.chars()
                .mapToObj(ch -> (char) ch)
                .collect(Collectors.toSet());

        if (chars1.isEmpty() && chars2.isEmpty()) {
            return 1.0;
        }

        if (chars1.isEmpty() || chars2.isEmpty()) {
            return 0.0;
        }

        Set<Character> intersection = new HashSet<>(chars1);
        intersection.retainAll(chars2);

        Set<Character> union = new HashSet<>(chars1);
        union.addAll(chars2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 构建结果DTO
     */
    private AssetScanImageDTO buildResultDTO(ProductItem product, AssetMatchResult matchResult) {
        String originalName = product.getRelatedTexts().stream()
                .map(OcrTextResult::getText)
                .collect(Collectors.joining(" "));

        BigDecimal originalAmount = product.getAmount();

        AssetScanImageDTO.AssetScanImageDTOBuilder builder = AssetScanImageDTO.builder()
                .amount(originalAmount)
                .originalAssetName(originalName)
                .cleanedAssetName(product.getCleanName())
                .confidence(product.getConfidence())
                .acquireTime(LocalDateTime.now())
                .recognitionTime(LocalDateTime.now());

        if (matchResult != null) {
            builder.assetNameId(matchResult.getAssetName().getId())
                    .assetName(matchResult.getAssetName().getName())
                    .matchedAssetName(matchResult.getAssetName().getName())
                    .matchScore(matchResult.getScore())
                    .isMatched(matchResult.getScore() >= confirmThreshold);
        } else {
            builder.assetNameId(null)
                    .assetName("未匹配: " + product.getCleanName())
                    .matchedAssetName(null)
                    .matchScore(0.0)
                    .isMatched(false);
        }

        return builder.build();
    }

    private List<AssetName> getUserAssets(String username) {
        LambdaQueryWrapper<AssetName> wrapper = Wrappers.lambdaQuery(AssetName.class)
                .eq(AssetName::getCreateUser, username)
                .eq(AssetName::getDeleted, 0)
                .select(AssetName::getId, AssetName::getName);

        return assetNameService.list(wrapper);
    }

    // ==================== 接口方法实现 ====================

    @Override
    public List<AssetScanImageDTO> matchAssetsFromTexts(List<String> extractedTexts, String username) {
        List<OcrTextResult> ocrTexts = extractedTexts.stream()
                .map(text -> {
                    OcrTextResult ocr = new OcrTextResult();
                    ocr.setText(text);
                    OcrTextResult.BoundingBox bbox = new OcrTextResult.BoundingBox();
                    bbox.setLeft(0.0);
                    bbox.setTop(0.0);
                    bbox.setRight(100.0);
                    bbox.setBottom(20.0);
                    ocr.setBbox(bbox);
                    return ocr;
                })
                .collect(Collectors.toList());

        return matchAssetsFromOcr(ocrTexts, username);
    }

    @Override
    public AssetScanImageDTO matchSingleText(String text, String username) {
        List<AssetScanImageDTO> results = matchAssetsFromTexts(Collections.singletonList(text), username);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public AssetScanImageDTO getBestMatch(List<String> extractedTexts, String username) {
        List<AssetScanImageDTO> matches = matchAssetsFromTexts(extractedTexts, username);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * 列聚类内部类
     */
    private static class ColumnCluster {
        private double center;
        private int count;
        private final List<Double> positions;

        public ColumnCluster(double firstPosition) {
            this.center = firstPosition;
            this.count = 1;
            this.positions = new ArrayList<>();
            this.positions.add(firstPosition);
        }

        public void add(double position) {
            positions.add(position);
            count++;
            center = positions.stream().mapToDouble(Double::doubleValue).average().orElse(center);
        }

        public double getCenter() { return center; }
        public int getCount() { return count; }
    }
}