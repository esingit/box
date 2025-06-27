package com.esin.box.service.impl.recognition;

import com.esin.box.dto.AssetScanImageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionContext {

    private final Scheme1Strategy scheme1;
    private final Scheme2Strategy scheme2;
    private final GeneralStructureStrategy general;

    // 保留老接口兼容
    private final HorizontalStructureStrategy horizontal;
    private final VerticalStructureStrategy vertical;

    /**
     * 新的方案分发方法 - 完全隔离
     */
    public List<AssetScanImageDTO> recognizeByScheme(String text, RecognitionScheme scheme) {
        log.info("执行{}识别", scheme.getDescription());

        switch (scheme) {
            case SCHEME_1:
                return scheme1.recognize(text);
            case SCHEME_2:
                return scheme2.recognize(text);
            case GENERAL:
                return general.recognize(text);
            default:
                log.warn("未知识别方案: {}, 使用通用方案", scheme);
                return general.recognize(text);
        }
    }

    /**
     * 兼容老接口的分发方法
     */
    @Deprecated
    public List<AssetScanImageDTO> dispatch(String text, Enum<?> format) {
        if (format.name().equals("HORIZONTAL")) return horizontal.recognize(text);
        if (format.name().equals("VERTICAL")) return vertical.recognize(text);
        return general.recognize(text);
    }
}