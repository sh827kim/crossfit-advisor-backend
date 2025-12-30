package org.spark.crossfit.dto.google;

import java.util.List;

public record VisionOcrResponse(
        List<VisionOcrResult> responses
) {}
