package com.aah.agent.decision;

import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.llm.LLMConfirmationService;
import com.aah.agent.risk.RiskContext;
import com.aah.agent.risk.RiskGateService;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.signal.TechnicalSignal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ChatClient.class)
public class TradeDecisionService {

    private final LLMConfirmationService llmConfirmationService;
    private final RiskGateService riskGateService;

    public TradeDecisionService(
            LLMConfirmationService llmConfirmationService,
            RiskGateService riskGateService
    ) {
        this.llmConfirmationService = llmConfirmationService;
        this.riskGateService = riskGateService;
    }

    public RiskResult evaluate(
            TechnicalSignal signal,
            RiskContext context
    ) {
        LLMConfirmation confirmation = llmConfirmationService.confirm(signal);

        if (!confirmation.confirmed()) {
            return RiskResult.rejected(
                    "LLM rejected the operation: " + confirmation.reasoning()
            );
        }

        return riskGateService.evaluate(context, confirmation);
    }
}
