package com.aah.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("llm")
public class LLMConfig {

    @Bean
    public ChatClient chatClient(Builder builder) {
        return builder.build();
    }
}
