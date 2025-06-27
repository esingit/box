package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class VerticalStrategy implements RecognitionStrategy {

    // 金额匹配模式
    private static final Pattern PRECISE_AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2}))");
    private static final Pattern LOOSE_AMOUNT_PATTERN = Pattern.compile("(\\d{4,}(?:\\.\\d{2})?)");

    // OCR错误纠正映射
    private static final Map<String, String> OCR_CORRECTIONS = Map.of(
            "讲添益", "鑫添益",
            "蠢尊利", "鑫尊利",
            "春添益", "鑫添益",
            "寺盈", "持盈",
            "恒窒", "恒睿",
            "恒蹇", "恒睿"
    );

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("方案1开始识别 - 结构化左右布局");

        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> results = new ArrayList<>();

        // 分析所有行的特征
        List<LineFeature> lineFeatures = analyzeLineFeatures(lines);

        // 找到所有候选的产品-金额对
        List<ProductCandidate> candidates = findProductCandidates(lineFeatures);

        // 构建最终的产品列表
        for (ProductCandidate candidate : candidates) {
            AssetScanImageDTO product = buildProduct(candidate);
            if (product != null) {
                results.add(product);
                log.debug("识别到产品: {} -> {}", product.getAssetName(), product.getAmount());
            }
        }

        log.debug("方案1识别完成，结果数量: {}", results.size());
        return results;
    }

    /**
     * 行特征分析
     */
    private static class LineFeature {
        int index;
        String text;
        double chineseRatio;        // 中文字符比例
        int chineseCount;           // 中文字符数量
        boolean hasAmount;          // 是否包含金额
        BigDecimal amount;          // 金额值
        boolean hasProductKeyword;  // 是否包含产品关键词
        int textLength;             // 文本长度
        boolean isAmountOnly;       // 是否只是纯金额
        boolean isEmpty;            // 是否为空或无效

        LineFeature(int index, String text) {
            this.index = index;
            this.text = text.trim();
            this.textLength = this.text.length();
            this.isEmpty = this.text.isEmpty() || isInvalidLine(this.text);

            if (!this.isEmpty) {
                this.chineseCount = countChinese(this.text);
                this.chineseRatio = this.textLength > 0 ? (double) this.chineseCount / this.textLength : 0;
                this.amount = extractAmount(this.text);
                this.hasAmount = this.amount != null;
                this.hasProductKeyword = RecognitionHelper.containsProductKeyword(this.text);
                this.isAmountOnly = this.text.matches("^[\\d,\\.]+$");
            }
        }

        private int countChinese(String text) {
            return (int) text.chars().filter(c -> c >= 0x4e00 && c <= 0x9fa5).count();
        }

        private BigDecimal extractAmount(String text) {
            // 先尝试精确匹配
            Matcher matcher = PRECISE_AMOUNT_PATTERN.matcher(text);
            BigDecimal lastValid = null;
            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replace(",", "");
                    BigDecimal value = new BigDecimal(amountStr);
                    if (value.compareTo(new BigDecimal("1000")) >= 0 &&
                            value.compareTo(new BigDecimal("400000")) <= 0) {
                        lastValid = value;
                    }
                } catch (Exception ignored) {}
            }

            if (lastValid != null) return lastValid;

            // 再尝试宽松匹配
            matcher = LOOSE_AMOUNT_PATTERN.matcher(text);
            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1);
                    BigDecimal value = new BigDecimal(amountStr);
                    if (value.compareTo(new BigDecimal("1000")) >= 0 &&
                            value.compareTo(new BigDecimal("400000")) <= 0) {
                        lastValid = value;
                    }
                } catch (Exception ignored) {}
            }

            return lastValid;
        }

        private boolean isInvalidLine(String text) {
            // 跳过明显的无效行
            if (text.matches("^\\d{1,2}:\\d{1,2}.*$")) return true; // 时间
            if (text.matches("^[<>《》\\(\\)=]+$")) return true; // 纯符号
            if (text.length() <= 2) return true; // 太短
            if (text.matches("^[a-zA-Z\\s]+$")) return true; // 纯英文
            return false;
        }
    }

    /**
     * 产品候选信息
     */
    private static class ProductCandidate {
        List<LineFeature> nameLines;
        LineFeature amountLine;
        BigDecimal amount;

        ProductCandidate(List<LineFeature> nameLines, LineFeature amountLine, BigDecimal amount) {
            this.nameLines = nameLines;
            this.amountLine = amountLine;
            this.amount = amount;
        }
    }

    /**
     * 分析所有行的特征
     */
    private List<LineFeature> analyzeLineFeatures(String[] lines) {
        List<LineFeature> features = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            LineFeature feature = new LineFeature(i, lines[i]);
            features.add(feature);

            if (!feature.isEmpty) {
                log.debug("行[{}] 中文:{} 比例:{:.2f} 金额:{} 关键词:{} 文本:[{}]",
                        i, feature.chineseCount, feature.chineseRatio,
                        feature.amount, feature.hasProductKeyword, feature.text);
            }
        }

        return features;
    }

    /**
     * 找到产品候选
     */
    private List<ProductCandidate> findProductCandidates(List<LineFeature> features) {
        List<ProductCandidate> candidates = new ArrayList<>();

        for (LineFeature feature : features) {
            if (feature.isEmpty || !feature.hasAmount) continue;

            // 为每个包含金额的行寻找对应的产品名称
            List<LineFeature> nameLines = findRelatedNameLines(features, feature);

            if (!nameLines.isEmpty()) {
                candidates.add(new ProductCandidate(nameLines, feature, feature.amount));
                log.debug("找到候选产品，金额行[{}]，名称行数:{}", feature.index, nameLines.size());
            }
        }

        return candidates;
    }

    /**
     * 寻找与金额行相关的名称行
     */
    private List<LineFeature> findRelatedNameLines(List<LineFeature> allFeatures, LineFeature amountLine) {
        List<LineFeature> nameLines = new ArrayList<>();

        // 1. 检查金额行本身是否包含名称信息
        if (amountLine.chineseCount > 3 && amountLine.hasProductKeyword) {
            nameLines.add(amountLine);
        }

        // 2. 向前搜索可能的名称行（最多3行）
        for (int i = amountLine.index - 1; i >= Math.max(0, amountLine.index - 3); i--) {
            LineFeature line = allFeatures.get(i);

            if (line.isEmpty) continue;

            // 如果遇到另一个包含大金额的行，停止
            if (line.hasAmount && line.amount.compareTo(new BigDecimal("1000")) > 0) {
                break;
            }

            // 判断是否是名称行
            if (isLikelyNameLine(line)) {
                nameLines.add(0, line); // 插入到前面保持顺序
            }
        }

        // 3. 向后搜索可能的名称行（最多2行）
        for (int i = amountLine.index + 1; i < Math.min(allFeatures.size(), amountLine.index + 3); i++) {
            LineFeature line = allFeatures.get(i);

            if (line.isEmpty) continue;

            // 如果遇到另一个包含大金额的行，停止
            if (line.hasAmount && line.amount.compareTo(new BigDecimal("1000")) > 0) {
                break;
            }

            // 判断是否是名称行
            if (isLikelyNameLine(line)) {
                nameLines.add(line);
            }
        }

        return nameLines;
    }

    /**
     * 判断是否像产品名称行
     */
    private boolean isLikelyNameLine(LineFeature line) {
        // 基本条件：有中文字符
        if (line.chineseCount < 2) return false;

        // 不能是纯金额行
        if (line.isAmountOnly) return false;

        // 包含产品关键词的优先级更高
        if (line.hasProductKeyword) return true;

        // 中文密度高的行
        if (line.chineseRatio > 0.5 && line.chineseCount >= 4) return true;

        // 长度适中且主要是中文的行
        if (line.textLength >= 6 && line.textLength <= 50 && line.chineseRatio > 0.3) return true;

        return false;
    }

    /**
     * 构建产品对象
     */
    private AssetScanImageDTO buildProduct(ProductCandidate candidate) {
        // 构建完整产品名称
        StringBuilder nameBuilder = new StringBuilder();

        for (LineFeature nameLine : candidate.nameLines) {
            if (nameLine == candidate.amountLine) {
                // 如果是金额行，需要移除金额部分
                String nameOnly = removeAmountFromText(nameLine.text);
                if (StringUtils.hasText(nameOnly)) {
                    nameBuilder.append(nameOnly);
                }
            } else {
                nameBuilder.append(nameLine.text);
            }
        }

        String fullName = nameBuilder.toString();

        // 验证产品名称
        if (!StringUtils.hasText(fullName) || fullName.length() < 4) {
            log.debug("产品名称太短或为空: [{}]", fullName);
            return null;
        }

        if (!RecognitionHelper.containsProductKeyword(fullName)) {
            log.debug("产品名称不包含关键词: [{}]", fullName);
            return null;
        }

        AssetScanImageDTO product = new AssetScanImageDTO();
        product.setOriginalAssetName(fullName);
        product.setCleanedAssetName(cleanProductName(fullName));
        product.setAssetName(cleanProductName(fullName));
        product.setAmount(candidate.amount);
        product.setAcquireTime(LocalDateTime.now());
        product.setRemark("图片识别导入");

        return product;
    }

    /**
     * 从文本中移除金额部分
     */
    private String removeAmountFromText(String text) {
        String result = text;

        // 移除精确格式的金额
        result = result.replaceAll("\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2}))\\s*", " ");

        // 移除宽松格式的金额
        result = result.replaceAll("\\s*(\\d{4,}(?:\\.\\d{2})?)\\s*", " ");

        return result.trim();
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
}