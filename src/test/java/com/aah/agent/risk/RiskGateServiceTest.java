package com.aah.agent.risk;

import com.aah.agent.llm.LLMConfirmation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskGateServiceTest {

    private final RiskGateService service = new RiskGateService(
            0.02,
            5,
            14,
            28,
            0.30
    );

    @Test
    void shouldAllowWhenAllRiskGatesPass() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.0
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertTrue(result.allowed());
    }

    @Test
    void shouldRejectWhenPositionExceedsMaxAccountRisk() {
        var context = new RiskContext(
                10_000.0,
                250.0,
                2,
                21,
                0.0
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("account risk"));
    }

    @Test
    void shouldRejectWhenDailyOperationLimitIsReached() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                5,
                21,
                0.0
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertFalse(result.allowed());
    }

    @Test
    void shouldRejectWhenExpirationIsOutsideAllowedWindow() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                7,
                0.0
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("expiration"));
    }

    @Test
    void shouldRejectWhenOptionLossExceedsStopLimit() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.31
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("stop"));
    }

    @Test
    void shouldRejectWhenLlmDoesNotConfirm() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.0
        );

        var result = service.evaluate(
                context,
                LLMConfirmation.rejected("Setup is not sufficiently bullish")
        );

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("LLM"));
    }

    @Test
    void shouldRejectWhenRiskContextIsMissing() {
        var result = service.evaluate(
                null,
                LLMConfirmation.confirmed("Bullish setup confirmed")
        );

        assertFalse(result.allowed());
    }

    @Test
    void shouldRejectWhenLlmConfirmationIsMissing() {
        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.0
        );

        var result = service.evaluate(context, null);

        assertFalse(result.allowed());
    }
}
