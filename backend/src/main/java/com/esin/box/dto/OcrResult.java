package com.esin.box.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OcrResult {
    private Boolean success;
    private String error;
    private OcrData data;

    @Data
    public static class OcrData {
        @JsonProperty("input_path")
        private String inputPath;

        @JsonProperty("total_text_regions")
        private Integer totalTextRegions;

        @JsonProperty("text_regions")
        private List<TextRegion> textRegions;

        @JsonProperty("sorted_text_regions")
        private List<TextRegion> sortedTextRegions;

        @JsonProperty("full_text")
        private String fullText;

        @JsonProperty("sorted_full_text")
        private String sortedFullText;

        @JsonProperty("confidence_stats")
        private ConfidenceStats confidenceStats;

        @JsonProperty("processing_info")
        private ProcessingInfo processingInfo;
    }

    @Data
    public static class TextRegion {
        private Integer id;
        private String text;
        private Double confidence;
        private BoundingBox bbox;
        private Point center;
        private Double angle;
        private List<List<Double>> points;
        private Double area;
        @JsonProperty("text_length")
        private Integer textLength;
        @JsonProperty("word_count")
        private Integer wordCount;
    }

    @Data
    public static class BoundingBox {
        private Double left;
        private Double top;
        private Double right;
        private Double bottom;
        private Double width;
        private Double height;
    }

    @Data
    public static class Point {
        private Double x;
        private Double y;
    }

    @Data
    public static class ConfidenceStats {
        @JsonProperty("average_confidence")
        private Double averageConfidence;

        @JsonProperty("min_confidence")
        private Double minConfidence;

        @JsonProperty("max_confidence")
        private Double maxConfidence;

        @JsonProperty("confidence_distribution")
        private ConfidenceDistribution confidenceDistribution;
    }

    @Data
    public static class ConfidenceDistribution {
        @JsonProperty("high_confidence")
        private Integer highConfidence;

        @JsonProperty("medium_confidence")
        private Integer mediumConfidence;

        @JsonProperty("low_confidence")
        private Integer lowConfidence;
    }

    @Data
    public static class ProcessingInfo {
        @JsonProperty("ocr_version")
        private String ocrVersion;

        private String language;

        @JsonProperty("total_characters")
        private Integer totalCharacters;

        @JsonProperty("total_words")
        private Integer totalWords;

        @JsonProperty("avg_text_length")
        private Double avgTextLength;
    }
}