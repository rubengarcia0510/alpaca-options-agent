package com.aah.agent.decision;

import org.springframework.ai.chat.client.ChatClient;
import com.aah.agent.llm.LLMConfirmation;
import org.springframework.ai.chat.client.ChatClient;
import com.aah.agent.llm.LLMConfirmationService;
import com.aah.agent.signal.TechnicalSignal;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Service
@ConditionalOnBean(ChatClient.class)
public class TradeDecisionService {

    private final LLMConfirmationService llmConfirmationService;

    public TradeDecisionService(LLMConfirmationService llmConfirmationService) {
        this.llmConfirmationService = llmConfirmationService;
    }

    public LLMConfirmation evaluate(TechnicalSignal signal) {
        return llmConfirmationService.confirm(signal);
    }
}
