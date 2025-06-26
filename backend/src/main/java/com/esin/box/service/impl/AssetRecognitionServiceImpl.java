package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.entity.AssetName;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.AssetRecognitionService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
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

    // 金额提取正则
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern LINE_END_AMOUNT_PATTERN = Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*$");

    // 关键词定义
    private static final Set<String> PRODUCT_KEYWORDS = Set.of(
            "理财", "基金", "债券", "存款", "产品", "固收", "开放", "净值",
            "添益", "尊利", "持盈", "鑫", "恒", "创利", "精选", "成长",
            "灵活", "稳健", "增利", "宝", "通", "汇", "融", "投"
    );

    // OCR错误纠正映射
    private static final Map<String, String> OCR_CORRECTIONS = Map.of(
            "讲添益", "鑫添益",
            "蠢尊利", "鑫尊利",
            "春添益", "鑫添益",
            "寺盈", "持盈",
            "恒窒", "恒睿",
            "产马", "产品",
            "产吕D", "产品"
    );

    @Override
    public List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception {
        log.info("开始处理图片识别，文件名: {}, 用户: {}", image.getOriginalFilename(), createUser);

        // 1. 执行OCR识别
        String ocrText = performOCR(image);
        if (!StringUtils.hasText(ocrText)) {
            log.warn("OCR识别结果为空");
            return new ArrayList<>();
        }

        // 2. 解析OCR文本
        List<AssetScanImageDTO> records = parseOCRText(ocrText);
        if (records.isEmpty()) {
            log.warn("未能从OCR文本中解析出任何记录");
            return records;
        }

        // 3. 匹配资产名称ID
        matchAssetNames(records, createUser);

        log.info("图片识别完成，共识别出 {} 条记录", records.size());
        return records;
    }

    /**
     * 执行OCR识别
     */
    private String performOCR(MultipartFile image) throws Exception {
        File tempFile = null;
        try {
            // 保存临时文件
            tempFile = saveToTempFile(image);

            // 读取图片
            BufferedImage bufferedImage = ImageIO.read(tempFile);
            if (bufferedImage == null) {
                throw new RuntimeException("无法读取图片文件");
            }

            // 执行OCR
            log.info("开始OCR识别...");
            CompletableFuture<String> ocrTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return tesseract.doOCR(bufferedImage);
                } catch (Exception e) {
                    throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
                }
            });

            String result = ocrTask.get(60, TimeUnit.SECONDS);
            log.info("OCR识别完成，文本长度: {}", result.length());
            return result;

        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 保存到临时文件
     */
    private File saveToTempFile(MultipartFile image) throws Exception {
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "asset_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
        File tempFile = new File(dir, fileName);
        image.transferTo(tempFile);
        return tempFile;
    }

    /**
     * 解析OCR文本
     */
    private List<AssetScanImageDTO> parseOCRText(String text) {
        List<AssetScanImageDTO> results = new ArrayList<>();
        String[] lines = text.split("\\n");

        log.info("开始解析OCR文本，共 {} 行", lines.length);

        // 查找有效数据的起始位置
        int dataStartIndex = findDataStartIndex(lines);

        // 逐行解析
        for (int i = dataStartIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (shouldSkipLine(line)) continue;

            // 尝试从当前行提取记录
            AssetScanImageDTO record = extractRecordFromLine(line, lines, i);
            if (record != null) {
                results.add(record);
                log.info("成功提取记录: {} - {}", record.getAssetName(), record.getAmount());
            }
        }

        return results;
    }

    /**
     * 查找数据开始的位置
     */
    private int findDataStartIndex(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 找到表头或特定标识后，数据从下一行开始
            if (line.contains("名称") && line.contains("金额")) {
                return i + 1;
            }
            // 或者找到第一个包含大额数字的行
            BigDecimal amount = extractAmountFromLine(line);
            if (amount != null && amount.compareTo(new BigDecimal("1000")) > 0) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 判断是否应该跳过该行
     */
    private boolean shouldSkipLine(String line) {
        if (line.isEmpty()) return true;

        // 跳过无关内容
        String[] skipPatterns = {
                "总金额", "收起", "温馨提示", "持仓市值", "持仓收益",
                "可赎回", "最短持有期", "每日可赎", "^郑", "^\\*+",
                "^\\d{2}:\\d{2}$", "^\\d{4}/\\d{2}/\\d{2}$"
        };

        for (String pattern : skipPatterns) {
            if (line.matches(pattern + ".*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从行中提取记录
     */
    private AssetScanImageDTO extractRecordFromLine(String line, String[] lines, int currentIndex) {
        // 1. 先检查当前行是否包含金额
        BigDecimal amount = extractAmountFromLine(line);

        if (amount != null && amount.compareTo(new BigDecimal("1000")) > 0) {
            // 提取产品名称
            String productName = extractProductName(line, lines, currentIndex);

            if (StringUtils.hasText(productName)) {
                AssetScanImageDTO record = new AssetScanImageDTO();
                record.setAssetName(cleanProductName(productName));
                record.setAmount(amount);
                record.setAcquireTime(LocalDateTime.now());
                record.setRemark("图片识别导入");
                return record;
            }
        }

        return null;
    }

    /**
     * 提取金额
     */
    private BigDecimal extractAmountFromLine(String line) {
        // 优先匹配行尾的金额
        Matcher matcher = LINE_END_AMOUNT_PATTERN.matcher(line);
        if (matcher.find()) {
            return parseAmount(matcher.group(1));
        }

        // 否则匹配任意位置的金额
        matcher = AMOUNT_PATTERN.matcher(line);
        if (matcher.find()) {
            return parseAmount(matcher.group(1));
        }

        return null;
    }

    /**
     * 提取产品名称
     */
    private String extractProductName(String line, String[] lines, int currentIndex) {
        // 如果当前行包含金额，提取金额前的部分作为名称
        String nameFromCurrentLine = extractNameBeforeAmount(line);

        StringBuilder fullName = new StringBuilder();
        if (StringUtils.hasText(nameFromCurrentLine)) {
            fullName.append(nameFromCurrentLine);
        }

        // 检查下面几行是否是名称的延续
        for (int i = currentIndex + 1; i < Math.min(currentIndex + 3, lines.length); i++) {
            String nextLine = lines[i].trim();
            if (shouldSkipLine(nextLine)) continue;

            // 如果下一行包含金额或其他产品标识，停止
            if (extractAmountFromLine(nextLine) != null) break;

            // 如果是产品名称的一部分，添加
            if (isProductNamePart(nextLine)) {
                fullName.append(nextLine);
            } else {
                break;
            }
        }

        return fullName.toString();
    }

    /**
     * 提取金额前的名称部分
     */
    private String extractNameBeforeAmount(String line) {
        Matcher matcher = Pattern.compile("^(.+?)\\s+[\\d,]+(?:\\.\\d{2})?\\s*$").matcher(line);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 判断是否是产品名称的一部分
     */
    private boolean isProductNamePart(String text) {
        // 必须包含中文
        if (!text.matches(".*[\\u4e00-\\u9fa5]+.*")) {
            return false;
        }

        // 包含产品关键词
        for (String keyword : PRODUCT_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        // 合理长度的中文文本
        return text.length() >= 2 && text.replaceAll("[^\\u4e00-\\u9fa5]", "").length() >= 2;
    }

    /**
     * 清理产品名称
     */
    private String cleanProductName(String name) {
        if (!StringUtils.hasText(name)) return "";

        String cleaned = name.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[':-]", "·")
                .replaceAll("[《》\"\"''（）()\\[\\]{}]", "");

        // 应用OCR纠正
        for (Map.Entry<String, String> entry : OCR_CORRECTIONS.entrySet()) {
            cleaned = cleaned.replace(entry.getKey(), entry.getValue());
        }

        return cleaned;
    }

    /**
     * 解析金额字符串
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            String cleaned = amountStr.replaceAll(",", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 匹配资产名称
     */
    private void matchAssetNames(List<AssetScanImageDTO> records, String createUser) {
        // 获取用户的所有资产名称
        List<AssetName> assetNames = getAssetNamesByUser(createUser);
        if (assetNames.isEmpty()) {
            log.warn("用户 {} 没有设置任何资产名称", createUser);
            return;
        }

        // 为每条记录匹配最佳资产名称
        for (AssetScanImageDTO record : records) {
            AssetName bestMatch = findBestMatchingAssetName(record.getAssetName(), assetNames);

            record.setAssetNameId(bestMatch.getId());
            record.setMatchedAssetName(bestMatch.getName());
            record.setOriginalAssetName(record.getAssetName());
            record.setIsMatched(true);

            // 计算相似度分数供参考
            double score = calculateSimilarity(record.getAssetName(), bestMatch.getName());
            record.setMatchScore(score);

            log.info("资产名称匹配: {} -> {} (ID: {}, 相似度: {:.2%})",
                    record.getAssetName(), bestMatch.getName(), bestMatch.getId(), score);
        }
    }

    /**
     * 获取用户的资产名称列表
     */
    private List<AssetName> getAssetNamesByUser(String createUser) {
        QueryWrapper<AssetName> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", createUser)
                .eq("deleted", 0)
                .orderByDesc("create_time");
        return assetNameService.list(wrapper);
    }

    /**
     * 找到最匹配的资产名称
     */
    private AssetName findBestMatchingAssetName(String recognizedName, List<AssetName> candidates) {
        String cleaned = cleanProductName(recognizedName);

        return candidates.stream()
                .map(candidate -> new AbstractMap.SimpleEntry<>(
                        candidate,
                        calculateSimilarity(cleaned, cleanProductName(candidate.getName()))
                ))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(candidates.get(0)); // 如果都不匹配，返回第一个
    }

    /**
     * 计算相似度（简化版）
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1.equals(str2)) return 1.0;

        // 检查包含关系
        if (str1.contains(str2) || str2.contains(str1)) {
            int minLen = Math.min(str1.length(), str2.length());
            int maxLen = Math.max(str1.length(), str2.length());
            return 0.8 * minLen / maxLen;
        }

        // 计算共同字符比例
        Set<Character> chars1 = str1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        Set<Character> chars2 = str2.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

        Set<Character> intersection = new HashSet<>(chars1);
        intersection.retainAll(chars2);

        Set<Character> union = new HashSet<>(chars1);
        union.addAll(chars2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}