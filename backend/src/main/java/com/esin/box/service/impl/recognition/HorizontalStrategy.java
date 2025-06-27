package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HorizontalStrategy implements RecognitionStrategy {

    // 日期匹配模式
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})"
    );

    // 金额匹配模式
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)"
    );

    // 日期格式化器
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d")
    };

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("横向方案开始识别 - 上下结构布局");

        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> results = new ArrayList<>();

        // 分析所有行的特征
        List<LineInfo> lineInfos = analyzeLines(lines);

        // 提取全局日期信息
        String globalEndDate = extractGlobalEndDate(lineInfos);
        log.debug("提取到全局结束日期: {}", globalEndDate);

        // 识别产品-金额对
        List<ProductPair> productPairs = identifyProductPairs(lineInfos);

        // 构建最终产品列表
        for (ProductPair pair : productPairs) {
            AssetScanImageDTO product = buildProduct(pair, globalEndDate);
            if (product != null) {
                results.add(product);
                log.debug("识别到产品: {} -> {} (结束日期: {})",
                        product.getAssetName(), product.getAmount(),
                        extractEndDateFromRemark(product.getRemark()));
            }
        }

        log.debug("横向方案识别完成，结果数量: {}", results.size());
        return results;
    }

    /**
     * 行信息分析
     */
    private static class LineInfo {
        int index;
        String text;
        boolean isProductName;
        boolean isAmount;
        boolean isDate;
        String extractedDate;
        BigDecimal amount;
        int chineseCount;

        LineInfo(int index, String text) {
            this.index = index;
            this.text = text.trim();
            this.chineseCount = countChinese(this.text);

            // 判断行类型
            this.isDate = containsDate(this.text);
            this.extractedDate = this.isDate ? extractDate(this.text) : null;
            this.isAmount = isPureAmountLine(this.text);
            this.amount = this.isAmount ? extractAmount(this.text) : null;
            this.isProductName = isProductNameLine(this.text);
        }

        private int countChinese(String text) {
            return (int) text.chars().filter(c -> c >= 0x4e00 && c <= 0x9fa5).count();
        }

        private boolean containsDate(String text) {
            return DATE_PATTERN.matcher(text).find();
        }

        private String extractDate(String text) {
            Matcher matcher = DATE_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        private boolean isPureAmountLine(String text) {
            // 匹配纯金额格式
            return text.matches("^\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?$");
        }

        private BigDecimal extractAmount(String text) {
            Matcher matcher = AMOUNT_PATTERN.matcher(text);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replace(",", "");
                    BigDecimal value = new BigDecimal(amountStr);
                    // 验证金额范围
                    if (value.compareTo(new BigDecimal("100")) >= 0 &&
                            value.compareTo(new BigDecimal("500000")) <= 0) {
                        return value;
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        private boolean isProductNameLine(String text) {
            // 包含产品关键词且有足够的中文字符
            return RecognitionHelper.containsProductKeyword(text) &&
                    this.chineseCount >= 3 &&
                    !this.isAmount &&
                    !this.isDate &&
                    !RecognitionHelper.shouldSkipLine(text);
        }
    }

    /**
     * 产品金额对
     */
    private static class ProductPair {
        LineInfo nameInfo;
        LineInfo amountInfo;
        String localEndDate; // 局部日期信息

        ProductPair(LineInfo nameInfo, LineInfo amountInfo, String localEndDate) {
            this.nameInfo = nameInfo;
            this.amountInfo = amountInfo;
            this.localEndDate = localEndDate;
        }
    }

    /**
     * 分析所有行
     */
    private List<LineInfo> analyzeLines(String[] lines) {
        List<LineInfo> lineInfos = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) continue;

            LineInfo info = new LineInfo(i, line);
            lineInfos.add(info);

            log.debug("行[{}] 类型: {} {} {} 文本: [{}]",
                    i,
                    info.isProductName ? "产品名" : "",
                    info.isAmount ? "金额" : "",
                    info.isDate ? "日期" : "",
                    info.text);
        }

        return lineInfos;
    }

    /**
     * 提取全局结束日期
     */
    private String extractGlobalEndDate(List<LineInfo> lineInfos) {
        // 优先查找文档开头的日期信息
        for (int i = 0; i < Math.min(5, lineInfos.size()); i++) {
            LineInfo info = lineInfos.get(i);
            if (info.isDate) {
                String formattedDate = formatDate(info.extractedDate);
                if (formattedDate != null) {
                    return formattedDate;
                }
            }
        }

        // 查找文档末尾的日期信息
        for (int i = Math.max(0, lineInfos.size() - 5); i < lineInfos.size(); i++) {
            LineInfo info = lineInfos.get(i);
            if (info.isDate) {
                String formattedDate = formatDate(info.extractedDate);
                if (formattedDate != null) {
                    return formattedDate;
                }
            }
        }

        return null;
    }

    /**
     * 识别产品-金额对
     */
    private List<ProductPair> identifyProductPairs(List<LineInfo> lineInfos) {
        List<ProductPair> pairs = new ArrayList<>();

        for (int i = 0; i < lineInfos.size(); i++) {
            LineInfo info = lineInfos.get(i);

            if (!info.isProductName) continue;

            // 向下查找对应的金额行（最多查找3行）
            LineInfo amountInfo = null;
            String localEndDate = null;

            for (int j = i + 1; j < Math.min(lineInfos.size(), i + 4); j++) {
                LineInfo nextInfo = lineInfos.get(j);

                // 找到金额行
                if (nextInfo.isAmount && amountInfo == null) {
                    amountInfo = nextInfo;
                }

                // 找到局部日期
                if (nextInfo.isDate && localEndDate == null) {
                    localEndDate = formatDate(nextInfo.extractedDate);
                }

                // 如果遇到下一个产品名称，停止查找
                if (nextInfo.isProductName && j > i + 1) {
                    break;
                }
            }

            if (amountInfo != null) {
                pairs.add(new ProductPair(info, amountInfo, localEndDate));
                log.debug("找到产品对: [{}] -> [{}] (局部日期: {})",
                        info.text, amountInfo.amount, localEndDate);
            }
        }

        return pairs;
    }

    /**
     * 构建产品对象
     */
    private AssetScanImageDTO buildProduct(ProductPair pair, String globalEndDate) {
        String productName = RecognitionHelper.cleanName(pair.nameInfo.text);

        if (!StringUtils.hasText(productName) || productName.length() < 4) {
            log.debug("产品名称无效: [{}]", productName);
            return null;
        }

        // 确定使用的结束日期（局部日期优先）
        String endDate = StringUtils.hasText(pair.localEndDate) ? pair.localEndDate : globalEndDate;

        // 构建备注信息
        String remark = buildRemark(endDate);

        AssetScanImageDTO product = new AssetScanImageDTO();
        product.setOriginalAssetName(pair.nameInfo.text);
        product.setCleanedAssetName(productName);
        product.setAssetName(productName);
        product.setAmount(pair.amountInfo.amount);
        product.setAcquireTime(LocalDateTime.now());
        product.setRemark(remark);

        return product;
    }

    /**
     * 构建备注信息
     */
    private String buildRemark(String endDate) {
        StringBuilder remarkBuilder = new StringBuilder("图片识别导入");

        if (StringUtils.hasText(endDate)) {
            remarkBuilder.append("，周期结束日：").append(endDate);
        }

        return remarkBuilder.toString();
    }

    /**
     * 格式化日期
     */
    private String formatDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) return null;

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    /**
     * 从备注中提取结束日期（用于日志显示）
     */
    private String extractEndDateFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) return "无";

        Pattern pattern = Pattern.compile("周期结束日：(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = pattern.matcher(remark);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "无";
    }
}