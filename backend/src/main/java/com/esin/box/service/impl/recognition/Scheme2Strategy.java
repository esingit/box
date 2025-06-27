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
public class Scheme2Strategy implements RecognitionStrategy {

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

            // 检查是否是金额行（行开头是金额）
            if (isAmountLineRelaxed(raw)) {
                BigDecimal amount = extractAmountFromStart(raw);
                if (!buffer.isEmpty() && amount != null && isValidProductAmount(amount)) {
                    String name = RecognitionHelper.cleanName(String.join("", buffer));

                    // 防止重复识别同一个产品
                    String productKey = name + "_" + amount;
                    if (!processedProducts.contains(productKey)) {
                        list.add(RecognitionHelper.createAssetDTO(name, amount));
                        processedProducts.add(productKey);
                        log.debug("识别到产品: {} -> {}", name, amount);
                    } else {
                        log.debug("跳过重复产品: {} -> {}", name, amount);
                    }
                    buffer.clear();
                }
            } else {
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
                // 处理OCR可能的错误：5.082.53 应该是 5,082.53
                if (amountStr.contains(".") && amountStr.indexOf('.') != amountStr.lastIndexOf('.')) {
                    // 有多个小数点，可能是OCR错误，将第一个点替换为逗号
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
     * 验证是否是有效的理财产品金额
     */
    private boolean isValidProductAmount(BigDecimal amount) {
        // 理财产品金额通常不会小于100元
        return amount.compareTo(new BigDecimal("100")) >= 0;
    }
}