package com.aah.agent.risk;

import com.aah.agent.llm.LLMConfirmation;

public class RiskGateService {

    private final double maxAccountRisk;
    private final int maxDailyOperations;
    private final int minDaysToExpiration;
    private final int maxDaysToExpiration;
    private final double maxOptionLossRatio;

    public RiskGateService(
            double maxAccountRisk,
            int maxDailyOperations,
            int minDaysToExpiration,
            int maxDaysToExpiration,
            double maxOptionLossRatio
    ) {
        this.maxAccountRisk = maxAccountRisk;
        this.maxDailyOperations = maxDailyOperations;
        this.minDaysToExpiration = minDaysToExpiration;
        this.maxDaysToExpiration = maxDaysToExpiration;
        this.maxOptionLossRatio = maxOptionLossRatio;
    }

    public RiskResult evaluate(
            RiskContext context,
            LLMConfirmation confirmation
    ) {
        if (confirmation == null || !confirmation.confirmed()) {
            return RiskResult.rejected("LLM did not confirm the operation");
        }

        if (context == null) {
            return RiskResult.rejected("Risk context is missing");
        }

        if (context.accountValue() <= 0 || context.optionCost() < 0) {
            return RiskResult.rejected("Invalid account risk data");
        }

        double accountRisk = context.optionCost() / context.accountValue();

        if (accountRisk > maxAccountRisk) {
            return RiskResult.rejected("Operation exceeds maximum account risk");
        }

        if (context.dailyOperations() >= maxDailyOperations) {
            return RiskResult.rejected("Daily operation limit reached");
        }

        if (context.daysToExpiration() < minDaysToExpiration
                || context.daysToExpiration() > maxDaysToExpiration) {
            return RiskResult.rejected("Option expiration is outside allowed window");
        }

        if (context.optionLossRatio() > maxOptionLossRatio) {
            return RiskResult.rejected("Option loss exceeds stop limit");
        }

        return RiskResult.approved();
    }
}
