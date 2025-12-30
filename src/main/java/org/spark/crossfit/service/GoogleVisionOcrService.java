package org.spark.crossfit.service;

import lombok.RequiredArgsConstructor;
import org.spark.crossfit.config.CustomApplicationConfig;
import org.spark.crossfit.dto.google.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

import static org.spark.crossfit.constants.CommonConstants.GOOGLE_VISION_API_HOST;
import static org.spark.crossfit.constants.CommonConstants.GOOGLE_VISION_API_PATH;

@Service
@RequiredArgsConstructor
public class GoogleVisionOcrService {
    private final CustomApplicationConfig config;
    private final RestClient restClient;


    public String detectText(byte[] imageBytes) {

        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        VisionOcrRequest request = new VisionOcrRequest(
                List.of(
                        new AnnotateImageRequest(
                                new VisionImage(base64),
                                List.of(new VisionFeature("TEXT_DETECTION"))
                        )
                )
        );

        VisionOcrResponse response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(GOOGLE_VISION_API_HOST)
                        .path(GOOGLE_VISION_API_PATH)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-goog-api-key", config.getGoogleApiKey())
                .body(request)
                .retrieve()
                .body(VisionOcrResponse.class);

        return extractText(response);
    }

    private String extractText(VisionOcrResponse response) {
        if (response == null || response.responses() == null || response.responses().isEmpty()) {
            return "";
        }

        VisionOcrResult result = response.responses().getFirst();

        // 1순위: textAnnotations[0]
        if (result.textAnnotations() != null && !result.textAnnotations().isEmpty()) {
            return safe(result.textAnnotations().getFirst().description());
        }

        // 2순위: fullTextAnnotation
        if (result.fullTextAnnotation() != null) {
            return safe(result.fullTextAnnotation().text());
        }

        return "";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
