package org.spark.crossfit.service;


import lombok.RequiredArgsConstructor;
import org.spark.crossfit.dto.MyInfo;
import org.spark.crossfit.dto.command.ChangeMyInfoCommand;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static org.spark.crossfit.util.SecurityUtil.getCurrentUserId;

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
                .workoutMinutes(member.getWorkoutMinutes())
                .additionalInfo(member.getAdditionalInfo())
                .build();
    }

    public void updateMyInfo(ChangeMyInfoCommand command) {

        var userId = getCurrentUserId();
        var member = memberService.findByEmail(userId).orElseThrow();

        if(command.nickname() != null) {
            member.changeNickname(command.nickname());
        }
        if(command.unitType() != null) {
            member.changeUnitType(command.unitType());
        }
        if(command.workoutMinutes() != null) {
            member.changeWorkoutMinutes(command.workoutMinutes());
        }

        if(command.additionalInfo() != null) {
            member.changeAdditionalInfo(command.additionalInfo());
        }

        memberService.updateMember(member);
    }
}
