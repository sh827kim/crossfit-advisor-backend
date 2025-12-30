package org.spark.crossfit.dto.google;


import java.util.List;

public record AnnotateImageRequest(
        VisionImage image,
        List<VisionFeature> features
) {}
