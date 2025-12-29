package org.spark.crossfit.controller;

import lombok.RequiredArgsConstructor;
import org.spark.crossfit.dto.CommonResult;
import org.spark.crossfit.dto.MyInfo;
import org.spark.crossfit.service.CrossfitAdvisorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrossfitAdvisorController {

    private final CrossfitAdvisorService crossfitAdvisorService;

    @GetMapping("/user/me")
    public CommonResult<MyInfo> getMyInfo() {
        var myInfo = crossfitAdvisorService.getMyInfo();
        return CommonResult.success(myInfo);
    }
}
