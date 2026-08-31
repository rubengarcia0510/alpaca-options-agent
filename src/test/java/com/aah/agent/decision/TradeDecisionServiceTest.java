package com.aah.agent.decision;

import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.llm.LLMConfirmationService;
import com.aah.agent.risk.RiskContext;
import com.aah.agent.risk.RiskGateService;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.signal.TechnicalSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeDecisionServiceTest {

    @Test
    void shouldApproveTradeWhenLlmConfirmsAndRiskGatesPass() {
        var llm = mock(LLMConfirmationService.class);
        var risk = mock(RiskGateService.class);

        var signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                602.0,
                "Short SMA above long SMA"
        );

        var confirmation = LLMConfirmation.confirmed("Setup confirmed");
        when(llm.confirm(signal)).thenReturn(confirmation);

        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.0
        );

        when(risk.evaluate(context, confirmation))
                .thenReturn(RiskResult.approved());

        var service = new TradeDecisionService(llm, risk);

        var result = service.evaluate(signal, context);

        assertTrue(result.allowed());
        assertEquals("All risk gates passed", result.reason());

        verify(llm).confirm(signal);
        verify(risk).evaluate(context, confirmation);
    }

    @Test
    void shouldRejectTradeWhenLlmRejects() {
        var llm = mock(LLMConfirmationService.class);
        var risk = mock(RiskGateService.class);

        var signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                602.0,
                "Short SMA above long SMA"
        );

        var confirmation = LLMConfirmation.rejected("Weak setup");
        when(llm.confirm(signal)).thenReturn(confirmation);

        var service = new TradeDecisionService(llm, risk);

        var context = new RiskContext(
                10_000.0,
                150.0,
                2,
                21,
                0.0
        );

        var result = service.evaluate(signal, context);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("LLM"));

        verify(llm).confirm(signal);
        verifyNoInteractions(risk);
    }

    @Test
    void shouldRejectTradeWhenRiskGateFails() {
        var llm = mock(LLMConfirmationService.class);
        var risk = mock(RiskGateService.class);

        var signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                602.0,
                "Short SMA above long SMA"
        );

        var confirmation = LLMConfirmation.confirmed("Setup confirmed");
        when(llm.confirm(signal)).thenReturn(confirmation);

        var context = new RiskContext(
                10_000.0,
                250.0,
                2,
                21,
                0.0
        );

        when(risk.evaluate(context, confirmation))
                .thenReturn(RiskResult.rejected("Operation exceeds maximum account risk"));

        var service = new TradeDecisionService(llm, risk);

        var result = service.evaluate(signal, context);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("account risk"));

        verify(llm).confirm(signal);
        verify(risk).evaluate(context, confirmation);
    }
}
