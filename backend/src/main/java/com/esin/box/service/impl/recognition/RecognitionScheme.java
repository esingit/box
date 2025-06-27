package com.esin.box.service.impl.recognition;

public enum RecognitionScheme {
    SCHEME_1("方案1"),
    SCHEME_2("方案2"),
    GENERAL("通用方案");

    private final String description;

    RecognitionScheme(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
