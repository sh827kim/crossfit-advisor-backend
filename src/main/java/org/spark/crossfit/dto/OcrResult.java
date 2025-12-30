package org.spark.crossfit.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String detectedText;
}
