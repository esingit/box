package com.esin.box.dto.ocr;

import lombok.Data;

import java.util.List;

/**
 * 列分析结果
 */
@Data
public class ColumnAnalysis {
    private List<ColumnCluster> columns;
    private ColumnCluster leftColumn;
    private ColumnCluster rightColumn;
    private double columnBoundary; // 列边界位置
}