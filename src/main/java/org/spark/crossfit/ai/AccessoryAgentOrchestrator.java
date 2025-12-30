package org.spark.crossfit.ai;


import lombok.RequiredArgsConstructor;
import org.spark.crossfit.dto.command.ChatCommand;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

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

        final Disposable[] disposable = new Disposable[1];
        emitter.onCompletion(() -> {
            if (disposable[0] != null && !disposable[0].isDisposed()) {
                disposable[0].dispose();
            }
        });

        emitter.onTimeout(() -> {
            if (disposable[0] != null && !disposable[0].isDisposed()) {
                disposable[0].dispose();
            }
        });
        emitter.onError((throwable) -> {
            if (disposable[0] != null && !disposable[0].isDisposed()) {
                disposable[0].dispose();
            }
        });

        disposable[0] = flux
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                content -> {
                        try {
                            emitter.send(SseEmitter.event().data(content));
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }, emitter::completeWithError, emitter::complete
        );

    }
}
