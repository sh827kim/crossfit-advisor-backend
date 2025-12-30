package org.spark.crossfit.dto.command;


import org.spark.crossfit.dto.UnitType;


public record ChangeMyInfoCommand(
        String nickname,
        UnitType unitType,
        Integer workoutMinutes,
        String additionalInfo
) {
}
