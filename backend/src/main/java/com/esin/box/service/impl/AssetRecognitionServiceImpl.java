package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.entity.AssetName;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.AssetRecognitionService;
import com.esin.box.service.impl.recognition.RecognitionContext;
import com.esin.box.service.impl.recognition.RecognitionHelper;
import com.esin.box.service.impl.recognition.RecognitionScheme;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AssetRecognitionServiceImpl implements AssetRecognitionService {

    @Autowired
    private ITesseract tesseract;
    @Autowired
    private AssetNameService assetNameService;
    @Autowired
    private RecognitionContext recognitionContext;

    @Value("${app.upload.temp-dir:/tmp/asset-images}")
    private String tempDir;

    @Override
    public List<AssetScanImageDTO> recognizeImage(MultipartFile image, String createUser) throws Exception {
        log.info("开始智能识别图片: {}", image.getOriginalFilename());

        // 1. OCR识别
        String ocrText = performOCR(image);
        if (!StringUtils.hasText(ocrText)) {
            return Collections.emptyList();
        }

        // 2. 智能判断使用哪个方案
        RecognitionScheme bestScheme = determineBestScheme(ocrText);
        log.info("智能选择方案: {}", bestScheme.getDescription());

        // 3. 执行识别
        List<AssetScanImageDTO> result = recognitionContext.recognizeByScheme(ocrText, bestScheme);

        // 4. 如果主方案识别失败，尝试降级方案
        if (result.isEmpty() && bestScheme != RecognitionScheme.GENERAL) {
            log.warn("主方案识别失败，尝试使用通用方案");
            result = recognitionContext.recognizeByScheme(ocrText, RecognitionScheme.GENERAL);
        }

        // 5. 匹配资产名称
        if (!result.isEmpty()) {
            matchAssetNames(result, createUser);
        }

        log.info("识别完成，共识别到{}条记录", result.size());
        return result;
    }

    /**
     * 智能判断最佳识别方案
     */
    /**
     * 智能判断最佳识别方案
     */
    private RecognitionScheme determineBestScheme(String ocrText) {
        String[] lines = ocrText.split("\\n");

        int leftRightScore = 0;  // 左右结构得分
        int upDownScore = 0;     // 上下结构得分

        log.debug("开始分析文本结构...");
        log.debug("OCR文本行数: {}", lines.length);

        // 打印前10行用于调试
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            log.debug("第{}行: [{}]", i, lines[i]);
        }

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // 更严格的左右结构判断：名称和金额在同一行，且金额在右侧
            if (isLeftRightPattern(line)) {
                leftRightScore += 3;
                log.debug("左右结构特征 +3: {}", line);
            }

            // 更准确的上下结构判断
            if (i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();
                if (isUpDownPattern(line, nextLine)) {
                    upDownScore += 3;
                    log.debug("上下结构特征 +3: {} -> {}", line, nextLine);
                }
            }

            // 检查纯金额行（上下结构的强特征）
            if (isPureAmountLine(line)) {
                upDownScore += 2;
                log.debug("纯金额行特征 +2: {}", line);
            }

            // 检查产品名称行（不包含金额）
            if (isPureProductNameLine(line)) {
                upDownScore += 1;
                log.debug("纯产品名称行 +1: {}", line);
            }
        }

        log.info("结构分析结果 - 左右结构得分: {}, 上下结构得分: {}", leftRightScore, upDownScore);

        // 根据得分选择方案
        if (upDownScore > leftRightScore && upDownScore >= 3) {
            return RecognitionScheme.SCHEME_2;  // 上下结构
        } else if (leftRightScore > upDownScore && leftRightScore >= 3) {
            return RecognitionScheme.SCHEME_1;  // 左右结构
        } else {
            log.info("无法确定明确结构特征，使用通用方案");
            return RecognitionScheme.GENERAL;   // 通用方案
        }
    }

    /**
     * 判断是否是左右结构模式
     */
    private boolean isLeftRightPattern(String line) {
        // 必须包含产品关键词
        if (!RecognitionHelper.containsProductKeyword(line)) {
            return false;
        }

        // 必须包含金额，且金额应该在行的右侧
        if (!RecognitionHelper.containsAmount(line)) {
            return false;
        }

        // 使用正则检查是否符合"名称 + 空格 + 金额"的模式
        return line.matches(".*[\\u4e00-\\u9fa5].*\\s+\\d+[,\\d]*(?:\\.\\d{2})?\\s*$");
    }

    /**
     * 判断是否是上下结构模式
     */
    private boolean isUpDownPattern(String currentLine, String nextLine) {
        // 当前行：包含产品关键词，不包含大金额
        boolean currentIsProductName = RecognitionHelper.containsProductKeyword(currentLine) &&
                !containsLargeAmount(currentLine);

        // 下一行：是纯金额行
        boolean nextIsAmount = isPureAmountLine(nextLine);

        return currentIsProductName && nextIsAmount;
    }

    /**
     * 判断是否是纯金额行
     */
    private boolean isPureAmountLine(String line) {
        // 匹配纯数字+逗号+小数点的格式，如 70,241.50
        return line.matches("^\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?$");
    }

    /**
     * 判断是否是纯产品名称行（包含产品关键词但不包含大金额）
     */
    private boolean isPureProductNameLine(String line) {
        return RecognitionHelper.containsProductKeyword(line) &&
                !containsLargeAmount(line);
    }

    /**
     * 判断是否包含大金额（用于区分产品名称和左右结构）
     */
    private boolean containsLargeAmount(String line) {
        // 查找行中的所有数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?");
        java.util.regex.Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            try {
                String amountStr = matcher.group().replace(",", "");
                java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
                // 大于1000的数字才认为是金额
                if (amount.compareTo(new java.math.BigDecimal("1000")) > 0) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    // ===== OCR阶段 =====
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

    // ===== 匹配资产名阶段 =====
    private void matchAssetNames(List<AssetScanImageDTO> list, String user) {
        List<AssetName> names = assetNameService.list(
                new QueryWrapper<AssetName>()
                        .eq("create_user", user)
                        .eq("deleted", 0)
                        .orderByDesc("create_time")
        );
        if (names.isEmpty()) return;

        JaroWinklerSimilarity sim = new JaroWinklerSimilarity();

        for (AssetScanImageDTO dto : list) {
            String cleaned = dto.getCleanedAssetName();
            AssetName best = names.stream()
                    .map(a -> Map.entry(a, sim.apply(normalize(cleaned), normalize(a.getName()))))
                    .max(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            double score = best == null ? 0.0 : sim.apply(normalize(cleaned), normalize(best.getName()));

            if (best != null) {
                dto.setAssetNameId(best.getId());
                dto.setMatchedAssetName(best.getName());
                dto.setAssetName(best.getName());
            }
            dto.setMatchScore(score);
            dto.setIsMatched(score >= 0.5);
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}]+", "")
                .toLowerCase();
    }
}