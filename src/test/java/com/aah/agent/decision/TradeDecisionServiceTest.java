package com.aah.agent.decision;

import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.llm.LLMConfirmationService;
import com.aah.agent.signal.TechnicalSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeDecisionServiceTest {

    @Test
    void shouldRejectTradeWhenLlmRejectsTechnicalSignal() {
        LLMConfirmationService llmConfirmationService =
                mock(LLMConfirmationService.class);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                601.0,
                "SMA(9) above SMA(21)"
        );

        when(llmConfirmationService.confirm(signal))
                .thenReturn(LLMConfirmation.rejected(
                        "Market context does not justify the trade."
                ));

        TradeDecisionService service =
                new TradeDecisionService(llmConfirmationService);

        LLMConfirmation result = service.evaluate(signal);

        assertFalse(result.confirmed());
        assertEquals(
                "Market context does not justify the trade.",
                result.reasoning()
        );

        verify(llmConfirmationService).confirm(signal);
    }

    @Test
    void shouldAllowTradeDecisionWhenLlmConfirmsTechnicalSignal() {
        LLMConfirmationService llmConfirmationService =
                mock(LLMConfirmationService.class);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                601.0,
                "SMA(9) above SMA(21)"
        );

        when(llmConfirmationService.confirm(signal))
                .thenReturn(LLMConfirmation.confirmed(
                        "Bullish setup confirmed."
                ));

        TradeDecisionService service =
                new TradeDecisionService(llmConfirmationService);

        LLMConfirmation result = service.evaluate(signal);

        assertTrue(result.confirmed());
        assertEquals(
                "Bullish setup confirmed.",
                result.reasoning()
        );

        verify(llmConfirmationService).confirm(signal);
    }
}
