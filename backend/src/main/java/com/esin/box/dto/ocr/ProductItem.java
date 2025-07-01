package com.esin.box.dto.ocr;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 产品项实体类
 */
@Data
public class ProductItem {
    /**
     * 原始产品名称
     */
    private String originalName;

    /**
     * 清理后的产品名称
     */
    private String cleanName;

    /**
     * 金额（保留原始精度）
     */
    private BigDecimal amount;

    /**
     * 金额文本对象
     */
    private OcrTextResult amountText;

    /**
     * 相关文本列表
     */
    private List<OcrTextResult> relatedTexts;

    /**
     * 置信度
     */
    private double confidence;

    /**
     * 页面Y坐标位置（用于排序）
     */
    private double pagePosition;

    /**
     * 行索引（用于排序）
     */
    private int rowIndex;

    /**
     * 构造函数
     */
    public ProductItem() {
        this.confidence = 0.0;
        this.pagePosition = 0.0;
        this.rowIndex = 0;
    }
}