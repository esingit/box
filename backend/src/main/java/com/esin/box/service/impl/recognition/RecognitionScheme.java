package com.esin.box.service.impl.recognition;

public enum RecognitionScheme {
    VERTICAL("竖向结构"),
    HORIZONTAL("横向结构"),
    COLUMN_BASED("分栏结构"),
    GENERAL("通用方案");

    private final String description;

    RecognitionScheme(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
