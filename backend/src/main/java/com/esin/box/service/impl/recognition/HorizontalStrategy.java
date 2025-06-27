package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HorizontalStrategy implements RecognitionStrategy {

    // 匹配行开头的金额模式
    private static final Pattern AMOUNT_START_PATTERN = Pattern.compile("^(\\d{1,3}(?:[,.]\\d{3})*(?:\\.\\d{2})?)");

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("方案2开始识别 - 上下结构专用");

        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        Set<String> processedProducts = new HashSet<>(); // 防重复

        for (String raw : lines) {
            raw = raw.trim();
            if (raw.isEmpty() || RecognitionHelper.shouldSkipLine(raw)) continue;

            // 检查是否是金额行
            if (isAmountLineRelaxed(raw)) {
                BigDecimal amount = extractAmountFromStart(raw);
                if (!buffer.isEmpty() && amount != null && isValidProductAmount(amount)) {
                    // 构建产品名称
                    String productName = buildProductName(buffer);

                    // 防止重复识别同一个产品
                    String productKey = productName + "_" + amount;
                    if (!processedProducts.contains(productKey)) {
                        list.add(RecognitionHelper.createAssetDTO(productName, amount));
                        processedProducts.add(productKey);
                        log.debug("识别到产品: {} -> {}", productName, amount);
                    }
                    buffer.clear();
                }
            } else {
                // 检查是否是新产品的开始
                if (isNewProductStart(raw) && !buffer.isEmpty()) {
                    // 如果是新产品开始，但还没遇到金额，说明可能是结构问题
                    log.debug("遇到新产品但未找到前一个产品的金额，可能不是上下结构");
                    buffer.clear();
                }

                // 只有包含产品关键词的行才加入缓冲区
                if (RecognitionHelper.containsProductKeyword(raw)) {
                    buffer.add(raw);
                }
            }
        }

        log.debug("方案2识别完成，结果数量: {}", list.size());
        return list;
    }

    /**
     * 判断是否是新产品的开始
     */
    private boolean isNewProductStart(String line) {
        // 以银行理财开头且长度适中的行
        return line.matches("^工银理财.*") && line.length() > 8;
    }

    /**
     * 构建产品名称，避免过度合并
     */
    private String buildProductName(List<String> buffer) {
        // 如果缓冲区太大，说明可能识别有误
        if (buffer.size() > 5) {
            log.warn("产品名称部分过多: {}", buffer.size());
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (String part : buffer) {
            nameBuilder.append(part);
        }

        return RecognitionHelper.cleanName(nameBuilder.toString());
    }

    /**
     * 放宽的金额行判断 - 行开头是金额即可
     */
    private boolean isAmountLineRelaxed(String line) {
        return AMOUNT_START_PATTERN.matcher(line).find();
    }

    /**
     * 从行开头提取金额
     */
    private BigDecimal extractAmountFromStart(String line) {
        Matcher matcher = AMOUNT_START_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                String amountStr = matcher.group(1);
                // 处理OCR可能的错误
                if (amountStr.contains(".") && amountStr.indexOf('.') != amountStr.lastIndexOf('.')) {
                    int firstDot = amountStr.indexOf('.');
                    amountStr = amountStr.substring(0, firstDot) + "," + amountStr.substring(firstDot + 1);
                }

                amountStr = amountStr.replace(",", "");
                return new BigDecimal(amountStr);
            } catch (Exception e) {
                log.debug("金额解析失败: {}", line);
                return null;
            }
        }
        return null;
    }

    /**
     * 验证是否是有效地理财产品金额
     */
    private boolean isValidProductAmount(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("100")) >= 0;
    }
}