package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.entity.AssetName;
import com.esin.box.service.AssetRecognitionService;
import com.esin.box.service.AssetNameService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssetRecognitionServiceImpl implements AssetRecognitionService {

    @Autowired
    private ITesseract tesseract;

    @Autowired
    private AssetNameService assetNameService;

    @Value("${app.upload.temp-dir:/tmp/asset-images}")
    private String tempDir;

    private static final Set<String> PRODUCT_KEYWORDS = Set.of(
            "理财", "基金", "债券", "存款", "产品", "固收", "开放", "净值",
            "添益", "尊利", "持盈", "鑫", "恒", "创利", "精选", "成长",
            "灵活", "稳健", "增利", "宝", "通", "汇", "融", "投"
    );

    private static final Map<String, String> OCR_CORRECTIONS = Map.ofEntries(
            Map.entry("讲添益", "鑫添益"), Map.entry("蠢尊利", "鑫尊利"),
            Map.entry("春添益", "鑫添益"), Map.entry("寺盈", "持盈"),
            Map.entry("恒窒", "恒睿"), Map.entry("产马", "产品"),
            Map.entry("产吕D", "产品")
    );

    private static final JaroWinklerSimilarity JW_SIM = new JaroWinklerSimilarity();
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[\\d.,]+");

    // 定义文本格式类型
    private enum TextFormat {
        VERTICAL,    // 上下结构
        HORIZONTAL,  // 左右结构
        UNKNOWN      // 未知格式
    }

    @Override
    public List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception {
        String ocrText = performOCR(image);
        if (!StringUtils.hasText(ocrText)) return Collections.emptyList();

        // 判断文本格式
        TextFormat format = detectTextFormat(ocrText);
        log.info("检测到文本格式: {}", format);

        List<AssetScanImageDTO> list;

        switch (format) {
            case VERTICAL:
                // 上下结构 - 使用方案二
                log.info("使用方案二（上下结构）进行解析...");
                list = parseMultiLineWithTrailingAmount(ocrText);
                break;
            case HORIZONTAL:
                // 左右结构 - 使用方案一
                log.info("使用方案一（左右结构）进行解析...");
                list = parseOCRText(ocrText);
                break;
            default:
                // 未知格式 - 使用通用方案
                log.info("使用通用方案进行解析...");
                list = parseGeneralFormat(ocrText);
                break;
        }

        // 如果识别失败，尝试其他方案
        if (list.isEmpty()) {
            log.warn("当前方案未识别出有效数据，尝试通用方案...");
            list = parseGeneralFormat(ocrText);
        }

        // 最后匹配资产名
        if (!list.isEmpty()) {
            matchAssetNames(list, createUser);
        }

        return list;
    }

    // 检测文本格式
    private TextFormat detectTextFormat(String text) {
        String[] lines = text.split("\\n");

        int verticalScore = 0;
        int horizontalScore = 0;

        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            String nextLine = lines[i + 1].trim();

            if (line.isEmpty() || nextLine.isEmpty()) continue;

            // 检测上下结构特征：产品名在上，金额在下
            if (containsProductKeyword(line) && !containsAmount(line)
                    && isAmountLine(nextLine)) {
                verticalScore += 3;
            }

            // 检测左右结构特征：同一行包含产品名和金额
            if (containsProductKeyword(line) && containsAmount(line)) {
                horizontalScore += 2;
            }

            // 检测金额独占一行的情况（上下结构特征）
            if (!line.matches(".*[\\u4e00-\\u9fa5].*") && isAmountLine(line)) {
                verticalScore++;
            }
        }

        log.debug("格式检测得分 - 上下结构: {}, 左右结构: {}", verticalScore, horizontalScore);

        if (verticalScore > horizontalScore && verticalScore >= 2) {
            return TextFormat.VERTICAL;
        } else if (horizontalScore > verticalScore && horizontalScore >= 2) {
            return TextFormat.HORIZONTAL;
        }

        return TextFormat.UNKNOWN;
    }

    // 检查是否包含产品关键词
    private boolean containsProductKeyword(String line) {
        return PRODUCT_KEYWORDS.stream().anyMatch(line::contains);
    }

    // 检查是否包含金额
    private boolean containsAmount(String line) {
        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        while (matcher.find()) {
            String amount = matcher.group();
            // 过滤掉年份、时间等
            if (amount.length() >= 4 && !amount.contains(".") && !amount.contains(",")) {
                continue;
            }
            try {
                BigDecimal value = new BigDecimal(normalizeAmountStr(amount));
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    // 通用解析方案
    private List<AssetScanImageDTO> parseGeneralFormat(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();

        // 策略1：尝试使用正则表达式匹配常见模式
        Pattern pattern1 = Pattern.compile("(.+?)([\\d,]+\\.?\\d*)$");
        Pattern pattern2 = Pattern.compile("(.+?)\\s+([\\d,]+\\.?\\d*)");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || shouldSkipLine(line)) continue;

            // 尝试匹配模式1：名称和金额在同一行
            Matcher matcher = pattern1.matcher(line);
            if (matcher.find()) {
                String name = cleanName(matcher.group(1));
                String amountStr = matcher.group(2);

                if (containsProductKeyword(name)) {
                    BigDecimal amount = parseAmount(normalizeAmountStr(amountStr));
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        AssetScanImageDTO dto = createAssetDTO(name, amount);
                        list.add(dto);
                        continue;
                    }
                }
            }

            // 尝试匹配模式2：名称和金额用空格分隔
            matcher = pattern2.matcher(line);
            if (matcher.find()) {
                String name = cleanName(matcher.group(1));
                String amountStr = matcher.group(2);

                if (containsProductKeyword(name)) {
                    BigDecimal amount = parseAmount(normalizeAmountStr(amountStr));
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        AssetScanImageDTO dto = createAssetDTO(name, amount);
                        list.add(dto);
                    }
                }
            }
        }

        // 策略2：如果上述方法没有找到结果，尝试智能匹配
        if (list.isEmpty()) {
            list = parseWithIntelligentMatching(lines);
        }

        return list;
    }

    // 智能匹配解析
    private List<AssetScanImageDTO> parseWithIntelligentMatching(String[] lines) {
        List<AssetScanImageDTO> list = new ArrayList<>();
        Map<String, BigDecimal> candidates = new HashMap<>();

        // 第一遍：收集所有可能的产品名称
        List<String> productNames = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || shouldSkipLine(line)) continue;

            if (containsProductKeyword(line) && !containsAmount(line)) {
                productNames.add(cleanName(line));
            }
        }

        // 第二遍：为每个产品名称查找最近的金额
        for (String productName : productNames) {
            BigDecimal amount = findNearestAmount(lines, productName);
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                candidates.put(productName, amount);
            }
        }

        // 创建结果
        for (Map.Entry<String, BigDecimal> entry : candidates.entrySet()) {
            AssetScanImageDTO dto = createAssetDTO(entry.getKey(), entry.getValue());
            list.add(dto);
        }

        return list;
    }

    // 查找最近的金额
    private BigDecimal findNearestAmount(String[] lines, String productName) {
        int productIndex = -1;

        // 找到产品名称所在的行
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(productName)) {
                productIndex = i;
                break;
            }
        }

        if (productIndex == -1) return null;

        // 先检查同一行
        BigDecimal sameLineAmount = extractAmountFromLine(lines[productIndex]);
        if (sameLineAmount != null) return sameLineAmount;

        // 向下查找（最多3行）
        for (int i = 1; i <= 3 && productIndex + i < lines.length; i++) {
            BigDecimal amount = extractAmountFromLine(lines[productIndex + i]);
            if (amount != null) return amount;
        }

        // 向上查找（最多2行）
        for (int i = 1; i <= 2 && productIndex - i >= 0; i++) {
            BigDecimal amount = extractAmountFromLine(lines[productIndex - i]);
            if (amount != null) return amount;
        }

        return null;
    }

    // 创建AssetScanImageDTO对象
    private AssetScanImageDTO createAssetDTO(String name, BigDecimal amount) {
        AssetScanImageDTO dto = new AssetScanImageDTO();
        dto.setOriginalAssetName(name);
        dto.setCleanedAssetName(name);
        dto.setAssetName(name);
        dto.setAmount(amount);
        dto.setAcquireTime(LocalDateTime.now());
        dto.setRemark("图片识别导入");
        return dto;
    }

    @Override
    public List<AssetScanImageDTO> recognizeImageAuto(MultipartFile image, String createUser) throws Exception {
        return recognizeAssetImage(image, createUser);
    }

    // ========== OCR 阶段 ==========
    private String performOCR(MultipartFile image) throws Exception {
        File tmp = saveToTempFile(image);
        try {
            BufferedImage buf = ImageIO.read(tmp);
            CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return tesseract.doOCR(buf);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return task.get(60, TimeUnit.SECONDS);
        } finally {
            if (tmp != null && tmp.exists()) tmp.delete();
        }
    }

    private File saveToTempFile(MultipartFile image) throws Exception {
        File dir = new File(tempDir);
        if (!dir.exists()) dir.mkdirs();
        File tmp = new File(dir, "asset_" + System.currentTimeMillis() + "_" + image.getOriginalFilename());
        image.transferTo(tmp);
        return tmp;
    }

    // ========== 方案一：左右结构解析 ==========
    private List<AssetScanImageDTO> parseOCRText(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        StringBuilder nameBuf = null;

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty() || shouldSkipLine(raw)) continue;

            if (isNameLine(raw)) {
                String part = cleanName(raw);
                if (nameBuf == null) {
                    nameBuf = new StringBuilder(part);
                } else if (!raw.matches(".*\\d.*") && raw.length() < nameBuf.length()) {
                    nameBuf.append(part);
                } else {
                    nameBuf = new StringBuilder(part);
                }
                continue;
            }

            if (nameBuf != null) {
                BigDecimal amount = findBestAmountInWindow(lines, i, 3);
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    String fullName = nameBuf.toString();
                    AssetScanImageDTO dto = createAssetDTO(fullName, amount);
                    list.add(dto);
                    nameBuf = null;
                }
            }
        }
        return list;
    }

    // ========== 方案二：上下结构解析 ==========
    private List<AssetScanImageDTO> parseMultiLineWithTrailingAmount(String text) {
        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        List<String> buffer = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty() || shouldSkipLine(raw)) continue;

            // 尝试判断是否是金额行（仅数字，无中文）
            if (isAmountLine(raw)) {
                BigDecimal amount = extractAmountFromLine(raw);
                if (!buffer.isEmpty() && amount != null) {
                    // 拼接前面的名称行
                    String name = cleanName(String.join("", buffer));
                    AssetScanImageDTO dto = createAssetDTO(name, amount);
                    list.add(dto);
                    buffer.clear(); // 清空缓存
                }
            } else {
                // 累加为名称部分
                buffer.add(raw);
            }
        }

        return list;
    }

    // ========== 辅助方法 ==========
    private boolean isNameLine(String line) {
        if (!line.matches(".*[\\u4e00-\\u9fa5].*")) return false;
        return PRODUCT_KEYWORDS.stream().anyMatch(line::contains)
                && !line.contains("撤单")
                && !line.contains("撤音")
                && !line.contains("持仓撤单");
    }

    private BigDecimal findBestAmountInWindow(String[] lines, int startIndex, int maxLines) {
        BigDecimal best = null;
        for (int j = startIndex; j < Math.min(lines.length, startIndex + maxLines); j++) {
            Matcher m = AMOUNT_PATTERN.matcher(lines[j]);
            while (m.find()) {
                String raw = m.group();
                String norm = normalizeAmountStr(raw);
                BigDecimal val = parseAmount(norm);
                if (val != null && (best == null || val.compareTo(best) > 0)) {
                    best = val;
                }
            }
        }
        return best;
    }

    private String normalizeAmountStr(String s) {
        s = s.replaceAll("\\s+", "").replace(",", "");
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0) {
            String intPart = s.substring(0, lastDot).replace(".", "");
            String fracPart = s.substring(lastDot + 1);
            return intPart + "." + fracPart;
        }
        return s;
    }

    private BigDecimal parseAmount(String s) {
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean shouldSkipLine(String line) {
        String[] skip = {
                "总金额", "收起", "温馨提示", "持仓市值", "持仓收益", "可赎回",
                "最短持有期", "每日可赎", "赎回类型"
        };
        for (String p : skip) if (line.startsWith(p)) return true;

        if (line.contains("持仓") && line.contains("撤单")) return true;
        if (line.equals("撤单") || line.equals("撤音")) return true;
        if (line.matches("^\\d{1,2}:\\d{1,2}$")) return true;
        if (line.matches("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*$")) return true;

        return false;
    }

    private String cleanName(String name) {
        String s = name.trim().replaceAll("\\s+", "")
                .replaceAll("[':-]", "·")
                .replaceAll("[《》\"''()\\[\\]{}]", "");
        for (var e : OCR_CORRECTIONS.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        return s;
    }

    private boolean isAmountLine(String line) {
        // 可能金额行是只包含数字、逗号、小数点，并且不能有中文
        return line.matches("^[\\d,\\.]+$");
    }

    private BigDecimal extractAmountFromLine(String line) {
        if (line == null || line.isBlank()) return null;

        // 提取所有数字串（含逗号、小数点）
        Matcher matcher = Pattern.compile("[\\d.,]+").matcher(line);
        BigDecimal lastValid = null;

        while (matcher.find()) {
            String raw = matcher.group();
            // 格式标准化
            String normalized = normalizeAmountStr(raw);
            try {
                BigDecimal value = new BigDecimal(normalized);
                lastValid = value;
            } catch (Exception ignored) {
            }
        }
        return lastValid;
    }

    // ========== 匹配阶段 ==========
    private void matchAssetNames(List<AssetScanImageDTO> list, String user) {
        List<AssetName> names = assetNameService.list(
                new QueryWrapper<AssetName>()
                        .eq("create_user", user)
                        .eq("deleted", 0)
                        .orderByDesc("create_time")
        );
        if (names.isEmpty()) return;

        for (AssetScanImageDTO dto : list) {
            String cleaned = dto.getCleanedAssetName();
            AssetName best = names.stream()
                    .map(a -> Map.entry(a, calculateSimilarity(cleaned, cleanName(a.getName()))))
                    .max(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            double score = best == null ? 0.0 : calculateSimilarity(cleaned, cleanName(best.getName()));

            if (best != null) {
                dto.setAssetNameId(best.getId());
                dto.setMatchedAssetName(best.getName());
                dto.setAssetName(best.getName());
            }
            dto.setMatchScore(score);
            dto.setIsMatched(score >= 0.5);
        }
    }

    // ========== 相似度计算 ==========
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String n1 = normalizeForCompare(s1);
        String n2 = normalizeForCompare(s2);
        return JW_SIM.apply(n1, n2);
    }

    private String normalizeForCompare(String s) {
        return s.replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}]+", "")
                .toLowerCase();
    }
}