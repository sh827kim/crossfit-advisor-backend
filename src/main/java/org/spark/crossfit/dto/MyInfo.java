package org.spark.crossfit.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MyInfo {
    private String email;
    private String nickname;
    private UnitType unitType;
    private Integer workoutMinutes;
    private String additionalInfo;
}
