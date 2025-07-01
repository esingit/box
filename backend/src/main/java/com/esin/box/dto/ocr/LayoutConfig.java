package com.esin.box.dto.ocr;

import java.math.BigDecimal;

/**
 * 布局检测和处理配置
 */
public final class LayoutConfig {

    // 列布局检测阈值
    public static final double COLUMN_ALIGNMENT_THRESHOLD = 0.6; // 列对齐度阈值
    public static final double COLUMN_WIDTH_RATIO = 0.2; // 列宽占比阈值
    public static final int MIN_COLUMN_ITEMS = 3; // 最小列项目数

    // 行布局检测阈值
    public static final double ROW_ALIGNMENT_THRESHOLD = 0.6; // 行对齐度阈值
    public static final double ROW_HEIGHT_TOLERANCE = 25.0; // 行高容差
    public static final int MIN_ROW_ITEMS = 2; // 最小行项目数

    // 垂直分组阈值
    public static final int MIN_VERTICAL_GROUP_SIZE = 2; // 最小垂直组大小

    // 通用阈值
    public static final double MIN_CONFIDENCE = 0.6;
    public static final int MIN_TEXT_LENGTH = 1;
    public static final int MAX_TEXT_LENGTH = 100;
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("10");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");
    public static final double MAX_VERTICAL_DISTANCE = 150.0;

}