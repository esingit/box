package com.esin.box.dto.ocr;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 垂直分组信息
 */
@Data
public class VerticalGroup {
    private List<OcrTextResult> texts = new ArrayList<>();
    private double centerX;
    private double topY;
    private double bottomY;
}