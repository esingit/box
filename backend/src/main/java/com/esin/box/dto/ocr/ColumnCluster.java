package com.esin.box.dto.ocr;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 列聚类信息
 */
@Data
public class ColumnCluster {
    private List<Double> positions = new ArrayList<>();
    private double center;

    public ColumnCluster(double initialPosition) {
        positions.add(initialPosition);
        center = initialPosition;
    }

    public void add(double position) {
        positions.add(position);
        updateCenter();
    }

    private void updateCenter() {
        center = positions.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
