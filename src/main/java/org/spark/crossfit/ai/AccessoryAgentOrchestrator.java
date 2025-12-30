package org.spark.crossfit.ai;


import lombok.RequiredArgsConstructor;
import org.spark.crossfit.auth.dto.StreamChunk;
import org.spark.crossfit.dto.command.ChatCommand;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class AccessoryAgentOrchestrator {
    private final ChatClient accessoryChatClient;

    public void stream(String conversationId, ChatCommand command, SseEmitter emitter) {
        var flux =accessoryChatClient.prompt()
                .user(command.message())
                .advisors(a -> a.param("Conversation-Id", conversationId))
                .stream()
                .content();

        AtomicReference<Disposable> subRef = new AtomicReference<>();

        Runnable dispose = () -> {
            Disposable d = subRef.get();
            if (d != null && !d.isDisposed()) d.dispose();
        };

        emitter.onCompletion(dispose);
        emitter.onTimeout(() -> { dispose.run(); emitter.complete(); });
        emitter.onError(e -> { dispose.run(); /* 여기서 completeWithError 하지 말기 */ });


        subRef.set(
                flux.publishOn(Schedulers.boundedElastic()) // send()가 블로킹/IO라 안전하게
                        .subscribe(
                                chunk -> {
                                    try {
                                        emitter.send(SseEmitter.event().name("message").data(new StreamChunk(chunk)));
                                    } catch (IOException clientGone) {
                                        // 탭 닫음/네트워크 끊김이 대부분 -> 조용히 종료
                                        dispose.run();
                                        emitter.complete();
                                    } catch (Exception any) {
                                        // 예외를 밖으로 던지지 말고 SSE를 닫아
                                        dispose.run();
                                        safeSend(emitter, "error", "처리 중 문제가 생겼습니다.");
                                        emitter.complete();
                                    }
                                },
                                err -> {
                                    // 여기서도 completeWithError 금지
                                    dispose.run();
                                    safeSend(emitter, "error", "처리 중 문제가 생겼습니다.");
                                    emitter.complete();
                                },
                                () -> {
                                    dispose.run();
                                    safeSend(emitter, "done", "");
                                    emitter.complete();
                                }
                        )
        );

    }
    private void safeSend(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception ignored) {
            // 이미 끊겼거나 커밋 상태면 무시
        }
    }
}
