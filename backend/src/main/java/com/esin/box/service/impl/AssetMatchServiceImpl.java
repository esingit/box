package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.dto.ocr.LayoutType;
import com.esin.box.dto.ocr.OcrTextResult;
import com.esin.box.dto.ocr.PageLayout;
import com.esin.box.dto.ocr.ProductItem;
import com.esin.box.entity.AssetName;
import com.esin.box.service.AssetMatchService;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.processor.ColumnLayoutProcessor;
import com.esin.box.service.processor.LayoutProcessor;
import com.esin.box.service.processor.RowLayoutProcessor;
import com.esin.box.utils.TextAnalysisUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
 * 支持多布局的资产匹配服务主实现类（修复版）
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

    @Value("${app.business.ocr.asset-match.confirm-threshold:0.65}")
    private double confirmThreshold;

    @Value("${app.business.ocr.asset-match.min-threshold:0.25}")
    private double minThreshold;

    @Value("${app.business.ocr.asset-match.min-display-threshold:0.60}")
    private double minDisplayThreshold; // 最低显示阈值，默认60%

    @Value("${app.business.ocr.asset-match.max-results:30}")
    private int maxResults;

    @Override
    public List<AssetScanImageDTO> matchAssetsFromOcr(List<OcrTextResult> ocrTexts, String username) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("=== 开始多布局资产匹配（修复版） ===");
        log.info("用户: {}, OCR文本数量: {}", username, ocrTexts.size());
        log.info("匹配度阈值: 确认={}%, 最低显示={}%",
                Math.round(confirmThreshold * 100), Math.round(minDisplayThreshold * 100));

        try {
            // 1. 获取用户资产
            List<AssetName> userAssets = getUserAssets(username);
            if (userAssets.isEmpty()) {
                log.warn("用户没有资产数据: {}", username);
                return Collections.emptyList();
            }

            // 2. 分析页面基础信息
            PageLayout pageLayout = analyzePageLayoutFixed(ocrTexts);

            // 3. 智能布局检测（修复版）
            LayoutDetectionResult detectionResult = detectLayoutFixed(pageLayout);
            log.info("检测到布局类型: {}, 置信度: {}", detectionResult.getLayoutType(), detectionResult.getConfidence());

            // 4. 选择处理器并处理
            List<ProductItem> products = processWithSelectedProcessor(pageLayout, detectionResult);
            log.info("处理得到产品项: {}", products.size());

            // 5. 产品验证和修复
            products = validateAndFixProductsAdvanced(products, pageLayout);
            log.info("验证修复后产品项: {}", products.size());

            // 6. 匹配资产（包含匹配度过滤）
            List<AssetScanImageDTO> results = matchProductsAdvanced(products, userAssets);
            log.info("匹配结果: {} (已过滤低于{}%匹配度的数据)",
                    results.size(), Math.round(minDisplayThreshold * 100));

            return results;

        } catch (Exception e) {
            log.error("资产匹配过程出现异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 修复的页面布局分析
     */
    private PageLayout analyzePageLayoutFixed(List<OcrTextResult> ocrTexts) {
        PageLayout layout = new PageLayout();

        // 过滤有效文本
        layout.setValidTexts(ocrTexts.stream()
                .filter(this::isValidTextFixed)
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
     * 修复的有效文本判断
     */
    private boolean isValidTextFixed(OcrTextResult text) {
        if (text == null || StringUtils.isBlank(text.getText())) {
            return false;
        }

        String content = text.getText().trim();

        // 基本长度检查
        if (content.isEmpty()) {
            return false;
        }

        // 排除明显无意义的字符
        return !content.matches("^[\\s\\-_=.]{2,}$");
    }

    /**
     * 修复的布局检测
     */
    private LayoutDetectionResult detectLayoutFixed(PageLayout layout) {
        if (layout.getValidTexts().size() < 4) {
            return new LayoutDetectionResult(LayoutType.TOP_BOTTOM_ROWS, 0.5);
        }

        // 检测策略1：基于金额位置分布
        LayoutDetectionResult amountBasedResult = detectLayoutByAmountDistribution(layout);

        // 检测策略2：基于文本密度
        LayoutDetectionResult densityBasedResult = detectLayoutByTextDensity(layout);

        // 选择置信度更高的结果
        if (amountBasedResult.getConfidence() > densityBasedResult.getConfidence()) {
            return amountBasedResult;
        } else {
            return densityBasedResult;
        }
    }

    /**
     * 基于金额分布检测布局
     */
    private LayoutDetectionResult detectLayoutByAmountDistribution(PageLayout layout) {
        List<OcrTextResult> amountTexts = layout.getValidTexts().stream()
                .filter(text -> isStrictAmountFormat(text.getText()))
                .toList();

        if (amountTexts.size() < 2) {
            return new LayoutDetectionResult(LayoutType.TOP_BOTTOM_ROWS, 0.3);
        }

        // 分析金额的X坐标分布
        double pageWidth = layout.getWidth();
        double rightBoundary = pageWidth * 0.6;

        long rightSideAmounts = amountTexts.stream()
                .filter(text -> text.getBbox().getLeft() > rightBoundary)
                .count();

        double rightSideRatio = (double) rightSideAmounts / amountTexts.size();

        // 增加对总金额的检测，如果检测到总金额，倾向于左右列布局
        boolean hasTotal = layout.getValidTexts().stream()
                .anyMatch(text -> text.getText().contains("总金额") || text.getText().contains("合计"));

        if (rightSideRatio >= 0.7 || (rightSideRatio >= 0.5 && hasTotal)) {
            return new LayoutDetectionResult(LayoutType.LEFT_RIGHT_COLUMNS, 0.6 + rightSideRatio * 0.3);
        } else {
            return new LayoutDetectionResult(LayoutType.TOP_BOTTOM_ROWS, 0.6 + (1 - rightSideRatio) * 0.3);
        }
    }

    /**
     * 基于文本密度检测布局
     */
    private LayoutDetectionResult detectLayoutByTextDensity(PageLayout layout) {
        // 分析文本的垂直分布密度
        Map<Integer, Integer> rowDensity = new HashMap<>();
        Map<Integer, Integer> columnDensity = new HashMap<>();

        for (OcrTextResult text : layout.getValidTexts()) {
            int rowKey = (int) (text.getBbox().getCenterY() / 50.0);
            int colKey = (int) (text.getBbox().getCenterX() / (layout.getWidth() / 4.0));

            rowDensity.merge(rowKey, 1, Integer::sum);
            columnDensity.merge(colKey, 1, Integer::sum);
        }

        // 计算密度方差
        double rowVariance = calculateVariance(rowDensity.values());
        double colVariance = calculateVariance(columnDensity.values());

        if (colVariance > rowVariance * 1.5) {
            return new LayoutDetectionResult(LayoutType.LEFT_RIGHT_COLUMNS, 0.6);
        } else {
            return new LayoutDetectionResult(LayoutType.TOP_BOTTOM_ROWS, 0.6);
        }
    }

    /**
     * 计算方差
     */
    private double calculateVariance(Collection<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
    }

    /**
     * 严格的金额格式判断 - 改进版
     */
    private boolean isStrictAmountFormat(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 排除日期格式
        if (trimmed.matches("^\\d{4}[/-]\\d{2}[/-]\\d{2}$")) {
            return false;
        }

        // 排除包含中文的（除了元）
        if (trimmed.matches(".*\\u4e00-\\u9fa5&&[^元].*")) {
            return false;
        }

        // 严格的金额格式（包括错误格式的处理）
        return trimmed.matches("^\\d{1,3}(,\\d{3})*(\\.\\d{2})?$") ||
                trimmed.matches("^\\d+\\.\\d{1,4}$") ||
                trimmed.matches("^\\d+\\.\\d{3}\\.\\d{2}$") || // 12.041.40
                (trimmed.matches("^\\d{1,8}$") && tryParseAsValidAmount(trimmed));
    }

    /**
     * 尝试解析为有效金额
     */
    private boolean tryParseAsValidAmount(String content) {
        try {
            double value = Double.parseDouble(content);
            return value >= 0.01 && value <= 100000000.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 使用选定的处理器处理
     */
    private List<ProductItem> processWithSelectedProcessor(PageLayout layout, LayoutDetectionResult detectionResult) {
        LayoutProcessor processor = selectProcessor(detectionResult.getLayoutType());
        List<ProductItem> products = processor.processLayout(layout);

        // 如果结果不理想，尝试备用处理器
        if (products.size() < 2 && detectionResult.getConfidence() < 0.8) {
            LayoutType alternativeType = detectionResult.getLayoutType() == LayoutType.TOP_BOTTOM_ROWS
                    ? LayoutType.LEFT_RIGHT_COLUMNS
                    : LayoutType.TOP_BOTTOM_ROWS;

            LayoutProcessor alternativeProcessor = selectProcessor(alternativeType);
            List<ProductItem> alternativeProducts = alternativeProcessor.processLayout(layout);

            if (alternativeProducts.size() > products.size()) {
                log.info("备用处理器产生了更好的结果: {} -> {}", products.size(), alternativeProducts.size());
                products = alternativeProducts;
            }
        }

        return products;
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
            default -> {
                log.info("使用默认上下行处理器");
                yield rowLayoutProcessor;
            }
        };
    }

    /**
     * 高级的产品验证和修复
     */
    private List<ProductItem> validateAndFixProductsAdvanced(List<ProductItem> products, PageLayout layout) {
        List<ProductItem> validatedProducts = new ArrayList<>();

        for (ProductItem product : products) {
            ProductItem fixedProduct = validateAndFixProductAdvanced(product, layout);
            if (fixedProduct != null) {
                validatedProducts.add(fixedProduct);
            }
        }

        return validatedProducts;
    }

    /**
     * 高级的单个产品验证和修复
     */
    private ProductItem validateAndFixProductAdvanced(ProductItem product, PageLayout layout) {
        // 修复产品名称
        if (StringUtils.isBlank(product.getCleanName()) ||
                product.getCleanName().startsWith("未知产品") ||
                product.getCleanName().startsWith("产品")) {

            String fixedName = tryToFixProductNameAdvanced(product, layout);
            if (StringUtils.isNotBlank(fixedName)) {
                product.setCleanName(fixedName);
                product.setOriginalName(fixedName);
            }
        }

        // 修复金额
        if (product.getAmount() == null || product.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal fixedAmount = tryToFixAmountAdvanced(product);
            if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
                product.setAmount(fixedAmount);
            }
        }

        // 基本验证
        if (StringUtils.isBlank(product.getCleanName()) || product.getCleanName().length() < 3) {
            log.debug("产品名称无效，丢弃产品: '{}'", product.getCleanName());
            return null;
        }

        if (product.getAmount() == null || product.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("金额无效，丢弃产品: '{}'", product.getCleanName());
            return null;
        }

        return product;
    }

    /**
     * 高级的产品名称修复
     */
    private String tryToFixProductNameAdvanced(ProductItem product, PageLayout layout) {
        // 策略1：从相关文本中提取
        if (product.getRelatedTexts() != null && !product.getRelatedTexts().isEmpty()) {
            String extractedName = extractProductNameFromRelatedTexts(product.getRelatedTexts());
            if (StringUtils.isNotBlank(extractedName) && extractedName.length() >= 6) {
                return textAnalysisUtil.cleanProductName(extractedName);
            }
        }

        // 策略2：从布局中寻找相关的产品名称文本
        if (product.getAmountText() != null) {
            String contextName = findProductNameFromContext(product.getAmountText(), layout.getValidTexts());
            if (StringUtils.isNotBlank(contextName)) {
                return textAnalysisUtil.cleanProductName(contextName);
            }
        }

        return null;
    }

    /**
     * 从相关文本中提取产品名称
     */
    private String extractProductNameFromRelatedTexts(List<OcrTextResult> relatedTexts) {
        List<String> candidates = relatedTexts.stream()
                .map(OcrTextResult::getText)
                .filter(StringUtils::isNotBlank)
                .filter(text -> !isStrictMetadata(text))
                .filter(text -> text.length() >= 4)
                .filter(this::isProductNameCandidate)
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        // 选择最长的候选
        return candidates.stream()
                .max(Comparator.comparing(String::length))
                .orElse(null);
    }

    /**
     * 从上下文中寻找产品名称
     */
    private String findProductNameFromContext(OcrTextResult amountText, List<OcrTextResult> allTexts) {
        double amountY = amountText.getBbox().getCenterY();

        // 在金额上方寻找产品名称
        List<OcrTextResult> textsAbove = allTexts.stream()
                .filter(text -> text.getBbox().getCenterY() < amountY)
                .filter(text -> Math.abs(text.getBbox().getCenterY() - amountY) <= 150.0)
                .filter(text -> !isStrictAmountFormat(text.getText()))
                .filter(text -> !isStrictMetadata(text.getText()))
                .filter(text -> isProductNameCandidate(text.getText()))
                .sorted(Comparator.comparingDouble(t -> Math.abs(t.getBbox().getCenterY() - amountY)))
                .limit(3)
                .toList();

        if (!textsAbove.isEmpty()) {
            return textsAbove.stream()
                    .map(OcrTextResult::getText)
                    .collect(Collectors.joining(""));
        }

        return null;
    }

    /**
     * 判断是否为产品名称候选
     */
    private boolean isProductNameCandidate(String text) {
        if (StringUtils.isBlank(text) || text.length() < 4) {
            return false;
        }

        return text.matches(".*[理财|基金|债券|产品|收益|固定|开放|净值|天天|添益|核心|优选|持盈].*") ||
                text.matches(".*[工银|交银|兴银|平安|招商|中信|建信|华夏].*") ||
                text.matches(".*[·||].*");
    }

    /**
     * 严格的元数据判断
     */
    private boolean isStrictMetadata(String content) {
        Set<String> strictMetadata = Set.of(
                "总金额", "总金额（元）", "名称", "金额", "理财", "收起", "展开", "温馨提示", "（元）",
                "产品持仓", "撤单", "持仓市值", "持仓币值", "可赎回日", "赎回类型", "赎回尖型",
                "今日可赎", "每日可赎", "预约赎回", "周期结束日", "可赎回开始日"
        );
        return strictMetadata.contains(content.trim());
    }

    /**
     * 高级的金额修复
     */
    private BigDecimal tryToFixAmountAdvanced(ProductItem product) {
        // 策略1：从金额文本重新解析
        if (product.getAmountText() != null) {
            BigDecimal amount = parseAmountAdvanced(product.getAmountText().getText());
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                return amount;
            }
        }

        // 策略2：从相关文本中寻找金额
        if (product.getRelatedTexts() != null) {
            for (OcrTextResult text : product.getRelatedTexts()) {
                if (isStrictAmountFormat(text.getText())) {
                    BigDecimal amount = parseAmountAdvanced(text.getText());
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        return amount;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 高级的金额解析
     */
    private BigDecimal parseAmountAdvanced(String content) {
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

            // 验证金额合理性
            if (amount.compareTo(BigDecimal.valueOf(0.01)) >= 0 &&
                    amount.compareTo(BigDecimal.valueOf(100000000.0)) <= 0) {
                return amount;
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }

        return null;
    }

    /**
     * 高级的产品匹配 - 包含匹配度过滤
     */
    private List<AssetScanImageDTO> matchProductsAdvanced(List<ProductItem> products, List<AssetName> userAssets) {
        List<AssetScanImageDTO> results = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            ProductItem product = products.get(i);

            AssetMatchResult bestMatch = findBestMatchAdvanced(product, userAssets);

            String displayAmount = product.getAmount() != null ?
                    textAnalysisUtil.formatAmountDisplay(product.getAmount()) : "0.00";

            if (bestMatch != null) {
                // 只记录匹配度达到阈值的产品
                if (bestMatch.getScore() >= minDisplayThreshold) {
                    AssetScanImageDTO dto = buildResultDTO(product, bestMatch);
                    results.add(dto);
                    log.info("产品 {} 匹配: '{}' -> '{}' (分数: {}%, 金额: {})",
                            i + 1, product.getCleanName(), bestMatch.getAssetName().getName(),
                            Math.round(bestMatch.getScore() * 100), displayAmount);
                } else {
                    log.info("产品 {} 匹配度过低，已过滤: '{}' -> '{}' (分数: {}%, 金额: {})",
                            i + 1, product.getCleanName(), bestMatch.getAssetName().getName(),
                            Math.round(bestMatch.getScore() * 100), displayAmount);
                }
            } else {
                log.warn("产品 {} 无匹配: '{}' (金额: {})",
                        i + 1, product.getCleanName(), displayAmount);
                // 无匹配的产品也不添加到结果中
            }
        }

        // 按匹配度降序排序
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        return results.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 高级的最佳匹配查找
     */
    private AssetMatchResult findBestMatchAdvanced(ProductItem product, List<AssetName> userAssets) {
        double bestScore = 0.0;
        AssetName bestAsset = null;

        String productName = product.getCleanName();

        for (AssetName asset : userAssets) {
            double score = calculateAdvancedSimilarity(productName, asset.getName());

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
     * 高级相似度计算
     */
    private double calculateAdvancedSimilarity(String text1, String text2) {
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
            return 0.75 + 0.2 * ratio;
        }

        // 多种相似度算法
        double jwSimilarity = jaroWinklerSimilarity.apply(clean1, clean2);
        double charSimilarity = calculateCharSimilarity(clean1, clean2);
        double keywordSimilarity = calculateKeywordSimilarity(clean1, clean2);

        return Math.max(Math.max(jwSimilarity, charSimilarity), keywordSimilarity);
    }

    /**
     * 字符相似度计算
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
     * 关键词相似度计算
     */
    private double calculateKeywordSimilarity(String text1, String text2) {
        Set<String> keywords1 = extractKeywords(text1);
        Set<String> keywords2 = extractKeywords(text2);

        if (keywords1.isEmpty() && keywords2.isEmpty()) {
            return 0.0;
        }

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        return (double) intersection.size() / union.size() * 0.9;
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        String[] productTypes = {"理财", "基金", "债券", "精选", "优选", "稳利", "创利", "灵活", "持盈", "添益"};
        String[] institutions = {"工银", "交银", "兴银", "平安", "招商", "中信", "建信", "华夏"};

        for (String keyword : productTypes) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        for (String keyword : institutions) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    /**
     * 构建结果DTO - 增强版，包含匹配度过滤
     */
    private AssetScanImageDTO buildResultDTO(ProductItem product, AssetMatchResult matchResult) {
        String originalName = product.getRelatedTexts() != null && !product.getRelatedTexts().isEmpty() ?
                product.getRelatedTexts().stream()
                        .map(OcrTextResult::getText)
                        .collect(Collectors.joining(" ")) :
                product.getOriginalName();

        BigDecimal originalAmount = product.getAmount() != null ? product.getAmount() : BigDecimal.ZERO;

        AssetScanImageDTO.AssetScanImageDTOBuilder builder = AssetScanImageDTO.builder()
                .amount(originalAmount)
                .originalAssetName(StringUtils.isNotBlank(originalName) ? originalName : product.getCleanName())
                .cleanedAssetName(product.getCleanName())
                .confidence(product.getConfidence())
                .acquireTime(LocalDateTime.now())
                .recognitionTime(LocalDateTime.now());

        if (matchResult != null && matchResult.getScore() >= minDisplayThreshold) {
            // 只有匹配度达到阈值的才设置匹配信息
            builder.assetNameId(matchResult.getAssetName().getId())
                    .assetName(matchResult.getAssetName().getName())
                    .matchedAssetName(matchResult.getAssetName().getName())
                    .matchScore(matchResult.getScore())
                    .isMatched(matchResult.getScore() >= confirmThreshold);
        } else {
            // 匹配度过低或无匹配，设置为未匹配状态
            builder.assetNameId(null)
                    .assetName(null)
                    .matchedAssetName(null)
                    .matchScore(matchResult != null ? matchResult.getScore() : 0.0)
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

    // 接口方法实现保持不变...
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
     * 布局检测结果内部类
     */
    @Getter
    private static class LayoutDetectionResult {
        private final LayoutType layoutType;
        private final double confidence;

        public LayoutDetectionResult(LayoutType layoutType, double confidence) {
            this.layoutType = layoutType;
            this.confidence = confidence;
        }

    }

    /**
     * 资产匹配结果内部类
     */
    @Setter
    @Getter
    private static class AssetMatchResult {
        private AssetName assetName;
        private double score;

    }
}