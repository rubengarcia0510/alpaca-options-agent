package com.aah.agent.llm;

import com.aah.agent.signal.TechnicalSignal;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LLMConfirmationServiceTest {

    @Test
    void shouldConfirmWhenLlmReturnsYes() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(
                "CONFIRMED: YES\nREASONING: Bullish SMA setup."
        );

        LLMConfirmationService service = new LLMConfirmationService(chatClient);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                601.0,
                "SMA(9) above SMA(21)"
        );

        LLMConfirmation result = service.confirm(signal);

        assertTrue(result.confirmed());
        assertEquals("Bullish SMA setup.", result.reasoning());
    }

    @Test
    void shouldRejectWhenLlmReturnsNo() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(
                "CONFIRMED: NO\nREASONING: Setup is not convincing."
        );

        LLMConfirmationService service = new LLMConfirmationService(chatClient);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                595.0,
                600.0,
                594.0,
                "SMA(9) below SMA(21)"
        );

        LLMConfirmation result = service.confirm(signal);

        assertFalse(result.confirmed());
        assertEquals("Setup is not convincing.", result.reasoning());
    }

    @Test
    void shouldRejectWhenThereIsNoTechnicalSignal() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);

        when(builder.build()).thenReturn(chatClient);

        LLMConfirmationService service = new LLMConfirmationService(chatClient);

        LLMConfirmation result = service.confirm(null);

        assertFalse(result.confirmed());
        assertEquals(
                "No technical signal available for LLM confirmation.",
                result.reasoning()
        );

        verifyNoInteractions(chatClient);
    }

    @Test
    void shouldRejectWhenLlmReturnsEmptyResponse() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(" ");

        LLMConfirmationService service = new LLMConfirmationService(chatClient);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                600.0,
                595.0,
                601.0,
                "SMA(9) above SMA(21)"
        );

        LLMConfirmation result = service.confirm(signal);

        assertFalse(result.confirmed());
        assertEquals("LLM returned an empty response.", result.reasoning());
    }
}
