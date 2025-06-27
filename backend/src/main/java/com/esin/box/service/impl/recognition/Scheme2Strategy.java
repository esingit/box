package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class Scheme2Strategy implements RecognitionStrategy {

    @Override
    public List<AssetScanImageDTO> recognize(String text) {
        log.debug("方案2开始识别 - 上下结构专用");

        String[] lines = text.split("\\n");
        List<AssetScanImageDTO> list = new ArrayList<>();

        // 只处理上下结构的数据
        if (!isVerticalStructure(lines)) {
            log.debug("不符合上下结构特征，方案2不处理");
            return list;
        }

        List<String> buffer = new ArrayList<>();

        for (String raw : lines) {
            raw = raw.trim();
            if (raw.isEmpty() || shouldSkipLineScheme2(raw)) continue;

            if (isAmountLineScheme2(raw)) {
                BigDecimal amount = extractAmountScheme2(raw);
                if (!buffer.isEmpty() && amount != null) {
                    String name = RecognitionHelper.cleanName(String.join("", buffer));
                    list.add(RecognitionHelper.createAssetDTO(name, amount));
                    buffer.clear();
                }
            } else {
                buffer.add(raw);
            }
        }

        log.debug("方案2识别完成，结果数量: {}", list.size());
        return list;
    }

    /**
     * 判断是否是上下结构
     */
    private boolean isVerticalStructure(String[] lines) {
        int verticalCount = 0;

        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            String nextLine = lines[i + 1].trim();

            if (line.isEmpty() || nextLine.isEmpty()) continue;

            // 产品名称在上，金额在下
            if (RecognitionHelper.containsProductKeyword(line) &&
                    !line.matches(".*\\d{3,}.*") && // 名称行不包含大数字
                    nextLine.matches("^[\\d,\\.]+$")) { // 下一行是纯金额
                verticalCount++;
            }
        }

        // 至少有2个上下结构的模式才认为是上下结构
        return verticalCount >= 2;
    }

    /**
     * 方案2专用的跳过行判断
     */
    private boolean shouldSkipLineScheme2(String line) {
        String[] skip = {
                "总金额", "收起", "温馨提示", "持仓市值", "持仓收益", "可赎回",
                "最短持有期", "每日可赎", "赎回类型"
        };

        for (String pattern : skip) {
            if (line.contains(pattern)) return true;
        }

        if (line.contains("撤单") || line.contains("撤音")) return true;

        return false;
    }

    /**
     * 方案2专用的金额行判断
     */
    private boolean isAmountLineScheme2(String line) {
        return line.matches("^[\\d,\\.]+$");
    }

    /**
     * 方案2专用的金额提取
     */
    private BigDecimal extractAmountScheme2(String line) {
        try {
            String normalized = RecognitionHelper.normalizeAmountStr(line);
            return new BigDecimal(normalized);
        } catch (Exception e) {
            return null;
        }
    }
}