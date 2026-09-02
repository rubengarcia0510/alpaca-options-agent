package com.aah.agent.execution;

import com.aah.agent.cli.AlpacaCliClient;
import com.aah.agent.orchestrator.OptionsDecision;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class OptionOrderExecutionService {

    private final AlpacaCliClient alpacaCliClient;
    private final Clock clock;

    public OptionOrderExecutionService(
            AlpacaCliClient alpacaCliClient,
            Clock clock
    ) {
        this.alpacaCliClient = alpacaCliClient;
        this.clock = clock;
    }

    public ExecutionResult execute(
            OptionsDecision decision,
            ExecutionAuthorization authorization,
            int quantity
    ) {
        if (decision == null) {
            return ExecutionResult.rejected(
                    null,
                    quantity,
                    "Decision is required"
            );
        }

        if (decision.optionCandidate() == null) {
            return ExecutionResult.rejected(
                    null,
                    quantity,
                    "Option candidate is required"
            );
        }

        String symbol = decision.optionCandidate().symbol();

        if (symbol == null || symbol.isBlank()) {
            return ExecutionResult.rejected(
                    symbol,
                    quantity,
                    "Option symbol is required"
            );
        }

        if (quantity <= 0) {
            return ExecutionResult.rejected(
                    symbol,
                    quantity,
                    "Quantity must be greater than zero"
            );
        }

        if (decision.riskResult() == null || !decision.riskResult().allowed()) {
            return ExecutionResult.rejected(
                    symbol,
                    quantity,
                    "Risk gates did not approve the decision"
            );
        }

        String fingerprint = createFingerprint(decision, quantity);

        if (authorization == null
                || !authorization.isValidFor(fingerprint, Instant.now(clock))) {
            return ExecutionResult.rejected(
                    symbol,
                    quantity,
                    "Explicit execution authorization is missing, expired, or does not match the decision"
            );
        }

        try {
            JsonNode response = alpacaCliClient.submitMarketOrder(
                    symbol,
                    "buy",
                    quantity
            );

            return ExecutionResult.executed(
                    symbol,
                    quantity,
                    response
            );
        } catch (Exception e) {
            return ExecutionResult.error(
                    symbol,
                    quantity,
                    "Alpaca CLI order submission failed: " + e.getMessage()
            );
        }
    }

    public String createFingerprint(
            OptionsDecision decision,
            int quantity
    ) {
        if (decision == null || decision.optionCandidate() == null) {
            throw new IllegalArgumentException(
                    "Decision and option candidate are required"
            );
        }

        var option = decision.optionCandidate();

        String payload = String.join("|",
                nullToEmpty(option.contractId()),
                nullToEmpty(option.symbol()),
                nullToEmpty(option.underlyingSymbol()),
                nullToEmpty(option.expirationDate() == null
                        ? null
                        : option.expirationDate().toString()),
                option.strikePrice() == null
                        ? ""
                        : option.strikePrice().toPlainString(),
                option.entryPrice() == null
                        ? ""
                        : option.entryPrice().toPlainString(),
                String.valueOf(option.multiplier()),
                "buy",
                String.valueOf(quantity)
        );

        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to create decision fingerprint",
                    e
            );
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
