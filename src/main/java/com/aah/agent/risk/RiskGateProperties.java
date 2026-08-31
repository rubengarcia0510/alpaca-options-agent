package com.aah.agent.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.risk")
public record RiskGateProperties(
        double maxAccountRisk,
        int maxDailyOperations,
        int minDaysToExpiration,
        int maxDaysToExpiration,
        double maxOptionLossRatio
) {
}
