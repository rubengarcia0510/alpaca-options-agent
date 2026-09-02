package com.aah.agent.orchestrator;

import com.aah.agent.cli.AlpacaCliClient;
import com.aah.agent.history.DecisionHistoryService;
import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.option.OptionCandidate;
import com.aah.agent.option.OptionContract;
import com.aah.agent.option.OptionLLMConfirmationService;
import com.aah.agent.option.OptionSelectionService;
import com.aah.agent.option.OptionSnapshot;
import com.aah.agent.risk.RiskGateProperties;
import com.aah.agent.risk.RiskGateService;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.signal.TechnicalSignal;
import com.aah.agent.signal.TechnicalSignalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OptionsAgentOrchestratorTest {

    private final TechnicalSignalService technicalSignalService =
            mock(TechnicalSignalService.class);

    private final AlpacaCliClient alpacaCliClient =
            mock(AlpacaCliClient.class);

    private final OptionSelectionService optionSelectionService =
            mock(OptionSelectionService.class);

    private final OptionLLMConfirmationService optionLLMConfirmationService =
            mock(OptionLLMConfirmationService.class);

    private final RiskGateService riskGateService =
            mock(RiskGateService.class);

    private final RiskGateProperties riskGateProperties =
            new RiskGateProperties(
                    0.02,
                    5,
                    14,
                    28,
                    0.30
            );

    private final DecisionHistoryService decisionHistoryService =
            new DecisionHistoryService();

    private final OptionsAgentOrchestrator orchestrator =
            new OptionsAgentOrchestrator(
                    technicalSignalService,
                    alpacaCliClient,
                    optionSelectionService,
                    optionLLMConfirmationService,
                    riskGateService,
                    riskGateProperties,
                    decisionHistoryService
            );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStopWhenThereIsNoTechnicalSignal() throws Exception {
        TechnicalSignal signal = new TechnicalSignal(
                "SPY",
                false,
                680.0,
                681.0,
                680.5,
                "SMA crossover not confirmed"
        );

        when(technicalSignalService.analyze("SPY"))
                .thenReturn(signal);

        OptionsDecision decision =
                orchestrator.evaluate("SPY");

        assertSame(signal, decision.signal());
        assertNull(decision.optionCandidate());
        assertNull(decision.llmConfirmation());
        assertNull(decision.riskResult());
        assertEquals("SMA crossover not confirmed", decision.reason());

        verifyNoInteractions(alpacaCliClient);
        verifyNoInteractions(optionSelectionService);
        verifyNoInteractions(optionLLMConfirmationService);
        verifyNoInteractions(riskGateService);
    }

    @Test
    void shouldStopWhenNoValidOptionCandidateExists() throws Exception {
        TechnicalSignal signal = bullishSignal();

        JsonNode contractsResponse =
                objectMapper.readTree("""
                    {
                      "option_contracts": []
                    }
                    """);

        when(technicalSignalService.analyze("SPY"))
                .thenReturn(signal);

        when(alpacaCliClient.getOptionContracts(
                eq("SPY"),
                anyString(),
                anyString(),
                eq("call"),
                eq(100)
        )).thenReturn(contractsResponse);

        when(alpacaCliClient.parseOptionContracts(contractsResponse))
                .thenReturn(List.of());

        when(optionSelectionService.select(
                eq(signal),
                eq(List.of()),
                eq(List.of()),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());

        OptionsDecision decision =
                orchestrator.evaluate("SPY");

        assertSame(signal, decision.signal());
        assertNull(decision.optionCandidate());
        assertNull(decision.llmConfirmation());
        assertNull(decision.riskResult());
        assertEquals(
                "No valid CALL option candidate available",
                decision.reason()
        );

        verify(optionSelectionService).select(
                eq(signal),
                eq(List.of()),
                eq(List.of()),
                any(LocalDate.class)
        );

        verifyNoInteractions(optionLLMConfirmationService);
        verifyNoInteractions(riskGateService);
    }

    @Test
    void shouldUseOnlyContractsReturnedByTheDteFilteredQuery() throws Exception {
        TechnicalSignal signal = bullishSignal();

        JsonNode contractsResponse =
                objectMapper.readTree("""
                    {
                      "option_contracts": []
                    }
                    """);

        OptionContract contract = contract();

        OptionSnapshot snapshot = snapshot();

        OptionCandidate candidate = candidate();

        when(technicalSignalService.analyze("SPY"))
                .thenReturn(signal);

        when(alpacaCliClient.getOptionContracts(
                eq("SPY"),
                anyString(),
                anyString(),
                eq("call"),
                eq(100)
        )).thenReturn(contractsResponse);

        when(alpacaCliClient.parseOptionContracts(contractsResponse))
                .thenReturn(List.of(contract));

        JsonNode quotesResponse =
                objectMapper.readTree("""
                    {
                      "quotes": {
                        "SPY260918C00680000": {
                          "ap": 4.25,
                          "bp": 4.10
                        }
                      }
                    }
                    """);

        when(alpacaCliClient.getOptionLatestQuotes(
                List.of("SPY260918C00680000")
        )).thenReturn(quotesResponse);

        when(alpacaCliClient.parseOptionLatestQuotes(quotesResponse))
                .thenReturn(List.of(snapshot));

        when(optionSelectionService.select(
                eq(signal),
                eq(List.of(contract)),
                eq(List.of(snapshot)),
                any(LocalDate.class)
        )).thenReturn(Optional.of(candidate));

        when(optionLLMConfirmationService.confirm(signal, candidate))
                .thenReturn(LLMConfirmation.confirmed("Bullish setup"));

        JsonNode accountResponse =
                objectMapper.readTree("""
                    {
                      "equity": "100000"
                    }
                    """);

        when(alpacaCliClient.getAccount())
                .thenReturn(accountResponse);

        when(riskGateService.evaluate(any(), any()))
                .thenReturn(RiskResult.approved());

        OptionsDecision decision =
                orchestrator.evaluate("SPY");

        assertSame(signal, decision.signal());
        assertSame(candidate, decision.optionCandidate());
        assertTrue(decision.llmConfirmation().confirmed());
        assertTrue(decision.riskResult().allowed());

        verify(alpacaCliClient).getOptionLatestQuotes(
                List.of("SPY260918C00680000")
        );

        verify(optionSelectionService).select(
                eq(signal),
                eq(List.of(contract)),
                eq(List.of(snapshot)),
                any(LocalDate.class)
        );

        verify(optionLLMConfirmationService)
                .confirm(signal, candidate);

        verify(riskGateService)
                .evaluate(any(), eq(decision.llmConfirmation()));
    }

    @Test
    void shouldRejectWhenLlmDoesNotConfirm() throws Exception {
        TechnicalSignal signal = bullishSignal();
        OptionContract contract = contract();
        OptionSnapshot snapshot = snapshot();
        OptionCandidate candidate = candidate();

        JsonNode contractsResponse =
                objectMapper.readTree("""
                    {
                      "option_contracts": []
                    }
                    """);

        JsonNode quotesResponse =
                objectMapper.readTree("""
                    {
                      "quotes": {
                        "SPY260918C00680000": {
                          "ap": 4.25,
                          "bp": 4.10
                        }
                      }
                    }
                    """);

        when(technicalSignalService.analyze("SPY"))
                .thenReturn(signal);

        when(alpacaCliClient.getOptionContracts(
                eq("SPY"),
                anyString(),
                anyString(),
                eq("call"),
                eq(100)
        )).thenReturn(contractsResponse);

        when(alpacaCliClient.parseOptionContracts(contractsResponse))
                .thenReturn(List.of(contract));

        when(alpacaCliClient.getOptionLatestQuotes(
                List.of("SPY260918C00680000")
        )).thenReturn(quotesResponse);

        when(alpacaCliClient.parseOptionLatestQuotes(quotesResponse))
                .thenReturn(List.of(snapshot));

        when(optionSelectionService.select(
                eq(signal),
                eq(List.of(contract)),
                eq(List.of(snapshot)),
                any(LocalDate.class)
        )).thenReturn(Optional.of(candidate));

        when(optionLLMConfirmationService.confirm(signal, candidate))
                .thenReturn(
                        LLMConfirmation.rejected("Setup is not convincing")
                );

        OptionsDecision decision =
                orchestrator.evaluate("SPY");

        assertFalse(decision.llmConfirmation().confirmed());
        assertFalse(decision.riskResult().allowed());
        assertTrue(decision.reason().contains("LLM rejected"));

        verifyNoInteractions(riskGateService);

        assertEquals(
                1,
                decisionHistoryService.countByStatus(
                        com.aah.agent.history.DecisionStatus.LLM_REJECTED
                )
        );
    }

    private TechnicalSignal bullishSignal() {
        return new TechnicalSignal(
                "SPY",
                true,
                682.0,
                680.0,
                681.0,
                "Short SMA crossed above long SMA"
        );
    }

    private OptionContract contract() {
        return new OptionContract(
                "contract-1",
                "SPY260918C00680000",
                "SPY",
                LocalDate.now().plusDays(17),
                new BigDecimal("680"),
                "call",
                "active",
                true,
                100
        );
    }

    private OptionSnapshot snapshot() {
        return new OptionSnapshot(
                "SPY260918C00680000",
                new BigDecimal("4.10"),
                new BigDecimal("4.25"),
                null,
                null
        );
    }

    private OptionCandidate candidate() {
        return new OptionCandidate(
                "contract-1",
                "SPY260918C00680000",
                "SPY",
                LocalDate.now().plusDays(17),
                17,
                new BigDecimal("680"),
                new BigDecimal("4.10"),
                new BigDecimal("4.25"),
                new BigDecimal("4.25"),
                null,
                null,
                100
        );
    }
}
