package com.esin.box.dto.ocr;

import java.math.BigDecimal;

/**
 * 布局检测和处理配置
 */
public final class LayoutConfig {

    // 通用阈值
    public static final double MIN_CONFIDENCE = 0.6;
    public static final int MIN_TEXT_LENGTH = 1;
    public static final int MAX_TEXT_LENGTH = 100;
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("10");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

}