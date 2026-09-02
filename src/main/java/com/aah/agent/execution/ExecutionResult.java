package com.aah.agent.execution;

import com.fasterxml.jackson.databind.JsonNode;

public record ExecutionResult(
        ExecutionStatus status,
        String symbol,
        int quantity,
        JsonNode brokerResponse,
        String reason
) {
    public static ExecutionResult rejected(
            String symbol,
            int quantity,
            String reason
    ) {
        return new ExecutionResult(
                ExecutionStatus.REJECTED,
                symbol,
                quantity,
                null,
                reason
        );
    }

    public static ExecutionResult executed(
            String symbol,
            int quantity,
            JsonNode brokerResponse
    ) {
        return new ExecutionResult(
                ExecutionStatus.EXECUTED,
                symbol,
                quantity,
                brokerResponse,
                "Order submitted successfully"
        );
    }

    public static ExecutionResult error(
            String symbol,
            int quantity,
            String reason
    ) {
        return new ExecutionResult(
                ExecutionStatus.ERROR,
                symbol,
                quantity,
                null,
                reason
        );
    }
}
