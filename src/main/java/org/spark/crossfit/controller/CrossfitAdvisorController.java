package org.spark.crossfit.controller;

import lombok.RequiredArgsConstructor;
import org.spark.crossfit.ai.AccessoryAgentOrchestrator;
import org.spark.crossfit.dto.CommonResult;
import org.spark.crossfit.dto.MyInfo;
import org.spark.crossfit.dto.command.ChangeMyInfoCommand;
import org.spark.crossfit.dto.command.ChatCommand;
import org.spark.crossfit.service.CrossfitAdvisorService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrossfitAdvisorController {

    private final CrossfitAdvisorService crossfitAdvisorService;
    private final AccessoryAgentOrchestrator orchestrator;

    @GetMapping("/user/me")
    public CommonResult<MyInfo> getMyInfo() {
        var myInfo = crossfitAdvisorService.getMyInfo();
        return CommonResult.success(myInfo);
    }

    @PutMapping("/user/me")
    public CommonResult<Void> updateMyInfo(@RequestBody ChangeMyInfoCommand command) {
        crossfitAdvisorService.updateMyInfo(command);
        return CommonResult.success(null);
    }



    @PostMapping("/accessory/chat")
    public SseEmitter chat(@RequestBody ChatCommand command, @RequestHeader(value = "Conversation-Id") String conversationId) {
        SseEmitter emitter = new SseEmitter(0L);

        orchestrator.stream(conversationId, command, emitter);

        return emitter;
    }
}
