package com.aah.agent.risk;

public record RiskResult(
        boolean allowed,
        String reason
) {
    public static RiskResult approved() {
        return new RiskResult(true, "All risk gates passed");
    }

    public static RiskResult rejected(String reason) {
        return new RiskResult(false, reason);
    }
}
