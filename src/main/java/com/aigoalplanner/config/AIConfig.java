package com.aigoalplanner.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OllamaOptions.builder()
                        .model("llama3.2")
                        .temperature(0.7)
                        .numPredict(2048)
                        .build())
                .build();
    }
}
