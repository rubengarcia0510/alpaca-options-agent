package com.aah.agent.execution;

import com.aah.agent.cli.AlpacaCliClient;
import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.orchestrator.OptionsDecision;
import com.aah.agent.option.OptionCandidate;
import com.aah.agent.signal.TechnicalSignal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptionOrderExecutionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-09-01T20:00:00Z");

    @Mock
    private AlpacaCliClient alpacaCliClient;

    private OptionOrderExecutionService service;

    private OptionsDecision approvedDecision;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new OptionOrderExecutionService(
                alpacaCliClient,
                clock
        );

        TechnicalSignal signal = new TechnicalSignal(
                "SPY",
                true,
                680.0,
                675.0,
                680.0,
                "Bullish SMA crossover"
        );

        OptionCandidate option = new OptionCandidate(
                "contract-123",
                "SPY260918C00680000",
                "SPY",
                LocalDate.of(2026, 9, 18),
                17,
                new BigDecimal("680"),
                new BigDecimal("79.84"),
                new BigDecimal("84.35"),
                new BigDecimal("84.35"),
                null,
                null,
                100
        );

        approvedDecision = new OptionsDecision(
                signal,
                option,
                LLMConfirmation.confirmed("Option is consistent with the signal"),
                RiskResult.approved(),
                "All decision gates passed"
        );
    }

    @Test
    void shouldRejectWhenDecisionIsNull() {
        ExecutionResult result = service.execute(
                null,
                ExecutionAuthorization.denied(),
                1
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldRejectWhenRiskGatesDidNotApprove() {
        OptionsDecision rejectedDecision = new OptionsDecision(
                approvedDecision.signal(),
                approvedDecision.optionCandidate(),
                approvedDecision.llmConfirmation(),
                RiskResult.rejected("Daily operation limit reached"),
                "Risk rejected"
        );

        String fingerprint = service.createFingerprint(
                rejectedDecision,
                1
        );

        ExecutionAuthorization authorization =
                new ExecutionAuthorization(
                        true,
                        fingerprint,
                        NOW.plusSeconds(300)
                );

        ExecutionResult result = service.execute(
                rejectedDecision,
                authorization,
                1
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldRejectWithoutExplicitAuthorization() {
        ExecutionResult result = service.execute(
                approvedDecision,
                ExecutionAuthorization.denied(),
                1
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        assertTrue(result.reason().contains("authorization"));

        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldRejectExpiredAuthorization() {
        String fingerprint = service.createFingerprint(
                approvedDecision,
                1
        );

        ExecutionAuthorization authorization =
                new ExecutionAuthorization(
                        true,
                        fingerprint,
                        NOW.minusSeconds(1)
                );

        ExecutionResult result = service.execute(
                approvedDecision,
                authorization,
                1
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldRejectAuthorizationForDifferentDecision() {
        String fingerprint = service.createFingerprint(
                approvedDecision,
                1
        );

        ExecutionAuthorization authorization =
                new ExecutionAuthorization(
                        true,
                        fingerprint,
                        NOW.plusSeconds(300)
                );

        ExecutionResult result = service.execute(
                approvedDecision,
                authorization,
                2
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldRejectInvalidQuantity() {
        ExecutionResult result = service.execute(
                approvedDecision,
                null,
                0
        );

        assertEquals(ExecutionStatus.REJECTED, result.status());
        assertTrue(result.reason().contains("Quantity"));

        verifyNoInteractions(alpacaCliClient);
    }

    @Test
    void shouldExecuteApprovedDecisionWithValidAuthorization()
            throws Exception {

        String fingerprint = service.createFingerprint(
                approvedDecision,
                2
        );

        ExecutionAuthorization authorization =
                new ExecutionAuthorization(
                        true,
                        fingerprint,
                        NOW.plusSeconds(300)
                );

        JsonNode brokerResponse =
                new ObjectMapper().readTree("""
                    {
                      "id": "order-123",
                      "status": "accepted"
                    }
                    """);

        when(alpacaCliClient.submitMarketOrder(
                "SPY260918C00680000",
                "buy",
                2
        )).thenReturn(brokerResponse);

        ExecutionResult result = service.execute(
                approvedDecision,
                authorization,
                2
        );

        assertEquals(ExecutionStatus.EXECUTED, result.status());
        assertEquals("SPY260918C00680000", result.symbol());
        assertEquals(2, result.quantity());
        assertEquals(brokerResponse, result.brokerResponse());

        verify(alpacaCliClient, times(1)).submitMarketOrder(
                "SPY260918C00680000",
                "buy",
                2
        );
    }

    @Test
    void shouldReturnErrorWhenAlpacaCliFails()
            throws Exception {

        String fingerprint = service.createFingerprint(
                approvedDecision,
                1
        );

        ExecutionAuthorization authorization =
                new ExecutionAuthorization(
                        true,
                        fingerprint,
                        NOW.plusSeconds(300)
                );

        when(alpacaCliClient.submitMarketOrder(
                "SPY260918C00680000",
                "buy",
                1
        )).thenThrow(new RuntimeException("CLI unavailable"));

        ExecutionResult result = service.execute(
                approvedDecision,
                authorization,
                1
        );

        assertEquals(ExecutionStatus.ERROR, result.status());
        assertTrue(result.reason().contains("CLI unavailable"));

        verify(alpacaCliClient, times(1)).submitMarketOrder(
                "SPY260918C00680000",
                "buy",
                1
        );
    }
}
