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
    public List<AssetScanImageDTO> recognizeWithScheme(MultipartFile image, String createUser, RecognitionScheme scheme) throws Exception {
        log.info("使用{}进行图片识别", scheme.getDescription());

        String ocrText = performOCR(image);
        if (!StringUtils.hasText(ocrText)) {
            return Collections.emptyList();
        }

        // 直接使用指定方案，不做自动检测和降级
        List<AssetScanImageDTO> result = recognitionContext.recognizeByScheme(ocrText, scheme);

        if (!result.isEmpty()) {
            matchAssetNames(result, createUser);
        }

        log.info("{}识别完成，识别到{}条记录", scheme.getDescription(), result.size());
        return result;
    }

    @Override
    @Deprecated
    public List<AssetScanImageDTO> recognizeAssetImage(MultipartFile image, String createUser) throws Exception {
        // 兼容老接口，默认使用通用方案
        return recognizeWithScheme(image, createUser, RecognitionScheme.GENERAL);
    }

    @Override
    @Deprecated
    public List<AssetScanImageDTO> recognizeImageAuto(MultipartFile image, String createUser) throws Exception {
        // 兼容老接口，保持原有的自动检测逻辑
        String ocrText = performOCR(image);
        if (!StringUtils.hasText(ocrText)) return Collections.emptyList();

        TextFormat format = detectTextFormat(ocrText);
        log.info("检测到文本格式: {}", format);

        List<AssetScanImageDTO> result = recognitionContext.dispatch(ocrText, format);
        if (result.isEmpty() && format != TextFormat.UNKNOWN) {
            log.warn("主策略识别失败，尝试使用通用方案");
            result = recognitionContext.dispatch(ocrText, TextFormat.UNKNOWN);
        }

        if (!result.isEmpty()) {
            matchAssetNames(result, createUser);
        }

        return result;
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

    // ===== 保留原有格式检测逻辑（兼容老接口） =====
    private enum TextFormat {
        VERTICAL, HORIZONTAL, UNKNOWN
    }

    private TextFormat detectTextFormat(String text) {
        // 保持原有逻辑不变...
        String[] lines = text.split("\\n");
        int verticalScore = 0;
        int horizontalScore = 0;

        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            String nextLine = lines[i + 1].trim();

            if (line.isEmpty() || nextLine.isEmpty()) continue;

            if (RecognitionHelper.containsProductKeyword(line) && !RecognitionHelper.containsAmount(line)
                    && RecognitionHelper.isAmountLine(nextLine)) {
                verticalScore += 3;
            }

            if (RecognitionHelper.containsProductKeyword(line) && RecognitionHelper.containsAmount(line)) {
                horizontalScore += 2;
            }

            if (!line.matches(".*[\\u4e00-\\u9fa5].*") && RecognitionHelper.isAmountLine(line)) {
                verticalScore++;
            }
        }

        log.debug("格式检测得分 - 上下结构: {}, 左右结构: {}", verticalScore, horizontalScore);

        if (verticalScore > horizontalScore && verticalScore >= 2) return TextFormat.VERTICAL;
        if (horizontalScore > verticalScore && horizontalScore >= 2) return TextFormat.HORIZONTAL;

        return TextFormat.UNKNOWN;
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