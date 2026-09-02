package com.aah.agent.execution;

import java.time.Instant;

public record ExecutionAuthorization(
        boolean authorized,
        String decisionFingerprint,
        Instant expiresAt
) {
    public static ExecutionAuthorization denied() {
        return new ExecutionAuthorization(false, null, null);
    }

    public boolean isValidFor(String expectedFingerprint, Instant now) {
        return authorized
                && decisionFingerprint != null
                && decisionFingerprint.equals(expectedFingerprint)
                && expiresAt != null
                && now.isBefore(expiresAt);
    }
}
