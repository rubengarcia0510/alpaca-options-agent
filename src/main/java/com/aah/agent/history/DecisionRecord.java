package com.aah.agent.history;

import java.time.Instant;

public record DecisionRecord(
        Instant timestamp,
        DecisionStatus status,
        String reasoning
) {
}
