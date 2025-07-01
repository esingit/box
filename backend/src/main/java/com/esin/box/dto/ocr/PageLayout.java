package com.esin.box.dto.ocr;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面布局信息
 */
@Data
public class PageLayout {
    private List<OcrTextResult> validTexts = new ArrayList<>();
    private double width;
    private double height;
    private double leftColumnBoundary;
    private double rightColumnBoundary;
}