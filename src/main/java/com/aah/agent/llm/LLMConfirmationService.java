package com.aah.agent.llm;

import com.aah.agent.signal.TechnicalSignal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Service
@ConditionalOnBean(ChatClient.class)
public class LLMConfirmationService {

    private final ChatClient chatClient;

    public LLMConfirmationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public LLMConfirmation confirm(TechnicalSignal signal) {
        if (signal == null || !signal.signal()) {
            return LLMConfirmation.rejected(
                    "No technical signal available for LLM confirmation."
            );
        }

        String prompt = """
                You are a cautious options trading assistant.

                A technical signal has detected a potential CALL option setup.

                Symbol: %s
                Price: %.4f
                Short SMA (9): %.4f
                Long SMA (21): %.4f
                Technical reason: %s

                Evaluate this setup independently.

                Confirm the operation only if the technical context provides
                a reasonable bullish setup. Otherwise reject it.

                Do not invent market data that is not provided.

                Return exactly two lines:
                CONFIRMED: YES or NO
                REASONING: concise explanation
                """.formatted(
                signal.symbol(),
                signal.price(),
                signal.shortSma(),
                signal.longSma(),
                signal.reason()
        );

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            return LLMConfirmation.rejected(
                    "LLM returned an empty response."
            );
        }

        boolean confirmed = response.lines()
                .anyMatch(line ->
                        line.trim().equalsIgnoreCase("CONFIRMED: YES"));

        String reasoning = response.lines()
                .filter(line ->
                        line.trim().regionMatches(
                                true, 0, "REASONING:", 0, 10))
                .map(line -> line.substring(10).trim())
                .findFirst()
                .orElse(response.trim());

        return confirmed
                ? LLMConfirmation.confirmed(reasoning)
                : LLMConfirmation.rejected(reasoning);
    }
}
