package com.aah.agent.option;

import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.signal.TechnicalSignal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ChatClient.class)
public class OptionLLMConfirmationService {

    private final ChatClient chatClient;

    public OptionLLMConfirmationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public LLMConfirmation confirm(
            TechnicalSignal signal,
            OptionCandidate option
    ) {
        if (signal == null || !signal.signal()) {
            return LLMConfirmation.rejected(
                    "No technical signal available for LLM confirmation."
            );
        }

        if (option == null) {
            return LLMConfirmation.rejected(
                    "No option candidate available for LLM confirmation."
            );
        }

        String prompt = """
                You are a cautious options trading assistant.

                A deterministic selection process has already selected
                a CALL option candidate for evaluation.

                You must NOT select or change the option contract.
                Evaluate only whether the proposed setup is reasonable.

                Technical signal:
                Symbol: %s
                Price: %.4f
                Short SMA (9): %.4f
                Long SMA (21): %.4f
                Technical reason: %s

                Proposed option:
                Contract: %s
                Expiration: %s
                DTE: %d
                Strike: %s
                Bid: %s
                Ask: %s
                Delta: %s

                Evaluate this setup independently.

                Confirm the operation only if the technical context and
                the proposed option provide a reasonable bullish setup.
                Otherwise reject it.

                Do not invent market data that is not provided.
                Do not propose another contract.

                Return exactly two lines:
                CONFIRMED: YES or NO
                REASONING: concise explanation
                """.formatted(
                signal.symbol(),
                signal.price(),
                signal.shortSma(),
                signal.longSma(),
                signal.reason(),
                option.symbol(),
                option.expirationDate(),
                option.daysToExpiration(),
                option.strikePrice(),
                option.bid(),
                option.ask(),
                option.delta()
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
