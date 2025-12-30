package org.spark.crossfit.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Configuration
public class AIConfig {

    @Bean
    public InMemoryChatMemoryRepository inMemoryChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatClient accessoryChatClient(
            ChatClient.Builder builder,
            InMemoryChatMemoryRepository chatMemoryRepository,
            @Value("classpath:prompts/system-prompt.st") Resource systemPromptResource
    ) {

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptResource);

        String currentDateWithDay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E요일)", Locale.KOREAN));

        String systemPromptText = systemPromptTemplate.render(
                Map.of(
                        "current_date", currentDateWithDay
                )
        );
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(15)
                .build();


        return builder
                .defaultSystem(systemPromptText)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }
}
