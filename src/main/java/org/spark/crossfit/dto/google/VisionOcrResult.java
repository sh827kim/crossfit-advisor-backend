package org.spark.crossfit.dto.google;


import java.util.List;

public record VisionOcrResult(
        List<TextAnnotation> textAnnotations,
        FullTextAnnotation fullTextAnnotation
) {}
