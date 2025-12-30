package org.spark.crossfit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.spark.crossfit.dto.UnitType;


@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String uid;
    private String nickname;
    @Enumerated(EnumType.STRING)
    private UnitType unitType;
    private Integer workoutMinutes;
    private String additionalInfo;

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public void changeWorkoutMinutes(Integer workoutMinutes) {
        this.workoutMinutes = workoutMinutes;
    }

    public void changeAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
