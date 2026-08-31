package com.aah.agent.decision;

import com.aah.agent.history.DecisionHistoryService;
import com.aah.agent.history.DecisionStatus;
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
    private final DecisionHistoryService decisionHistoryService;

    public TradeDecisionService(
            LLMConfirmationService llmConfirmationService,
            RiskGateService riskGateService,
            DecisionHistoryService decisionHistoryService
    ) {
        this.llmConfirmationService = llmConfirmationService;
        this.riskGateService = riskGateService;
        this.decisionHistoryService = decisionHistoryService;
    }

    public RiskResult evaluate(
            TechnicalSignal signal,
            RiskContext context
    ) {
        LLMConfirmation confirmation = llmConfirmationService.confirm(signal);

        if (!confirmation.confirmed()) {
            String reason = "LLM rejected the operation: " + confirmation.reasoning();

            decisionHistoryService.record(
                    DecisionStatus.LLM_REJECTED,
                    reason
            );

            return RiskResult.rejected(reason);
        }

        RiskResult result = riskGateService.evaluate(context, confirmation);

        if (result.allowed()) {
            decisionHistoryService.record(
                    DecisionStatus.APPROVED,
                    result.reason()
            );
        } else {
            decisionHistoryService.record(
                    DecisionStatus.RISK_REJECTED,
                    result.reason()
            );
        }

        return result;
    }
}
