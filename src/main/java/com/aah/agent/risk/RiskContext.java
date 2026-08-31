package com.aah.agent.risk;

public record RiskContext(
        double accountValue,
        double optionCost,
        int dailyOperations,
        int daysToExpiration,
        double optionLossRatio
) {
}
