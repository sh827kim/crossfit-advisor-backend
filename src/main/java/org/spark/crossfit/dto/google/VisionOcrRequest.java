package org.spark.crossfit.dto.google;

import java.util.List;

public record VisionOcrRequest(
        List<AnnotateImageRequest> requests
) {}
