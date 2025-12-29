package org.spark.crossfit.service;


import lombok.RequiredArgsConstructor;
import org.spark.crossfit.dto.MyInfo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrossfitAdvisorService {

    private final MemberService memberService;

    public MyInfo getMyInfo() {
        var userId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        var member = memberService.findByEmail(userId).orElseThrow();
        return MyInfo.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .unitType(member.getUnitType())
                .preferences(
                        MyInfo.Preferences.builder()
                                .workoutMinutes(member.getWorkoutMinutes())
                                .additionalInfo(member.getAdditionalInfo())
                                .build()
                )
                .build();
    }
}
