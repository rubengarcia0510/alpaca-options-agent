package com.aah.agent.orchestrator;

import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.option.OptionCandidate;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.signal.TechnicalSignal;

public record OptionsDecision(
        TechnicalSignal signal,
        OptionCandidate optionCandidate,
        LLMConfirmation llmConfirmation,
        RiskResult riskResult,
        String reason
) {
}
