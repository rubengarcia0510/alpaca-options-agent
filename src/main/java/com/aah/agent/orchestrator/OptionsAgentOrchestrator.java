package com.aah.agent.orchestrator;

import com.aah.agent.cli.AlpacaCliClient;
import com.aah.agent.history.DecisionHistoryService;
import com.aah.agent.llm.LLMConfirmation;
import com.aah.agent.option.OptionCandidate;
import com.aah.agent.option.OptionContract;
import com.aah.agent.option.OptionLLMConfirmationService;
import com.aah.agent.option.OptionSelectionService;
import com.aah.agent.option.OptionSnapshot;
import com.aah.agent.risk.RiskContext;
import com.aah.agent.risk.RiskGateProperties;
import com.aah.agent.risk.RiskGateService;
import com.aah.agent.risk.RiskResult;
import com.aah.agent.signal.TechnicalSignal;
import com.aah.agent.signal.TechnicalSignalService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(ChatClient.class)
public class OptionsAgentOrchestrator {

    private final TechnicalSignalService technicalSignalService;
    private final AlpacaCliClient alpacaCliClient;
    private final OptionSelectionService optionSelectionService;
    private final OptionLLMConfirmationService optionLLMConfirmationService;
    private final RiskGateService riskGateService;
    private final RiskGateProperties riskGateProperties;
    private final DecisionHistoryService decisionHistoryService;

    public OptionsAgentOrchestrator(
            TechnicalSignalService technicalSignalService,
            AlpacaCliClient alpacaCliClient,
            OptionSelectionService optionSelectionService,
            OptionLLMConfirmationService optionLLMConfirmationService,
            RiskGateService riskGateService,
            RiskGateProperties riskGateProperties,
            DecisionHistoryService decisionHistoryService
    ) {
        this.technicalSignalService = technicalSignalService;
        this.alpacaCliClient = alpacaCliClient;
        this.optionSelectionService = optionSelectionService;
        this.optionLLMConfirmationService = optionLLMConfirmationService;
        this.riskGateService = riskGateService;
        this.riskGateProperties = riskGateProperties;
        this.decisionHistoryService = decisionHistoryService;
    }

    public OptionsDecision evaluate(String symbol) throws Exception {
        TechnicalSignal signal = technicalSignalService.analyze(symbol);

        if (!signal.signal()) {
            return new OptionsDecision(
                    signal,
                    null,
                    null,
                    null,
                    signal.reason()
            );
        }

        LocalDate today = LocalDate.now();
        LocalDate minExpiration =
                today.plusDays(riskGateProperties.minDaysToExpiration());
        LocalDate maxExpiration =
                today.plusDays(riskGateProperties.maxDaysToExpiration());

        JsonNode contractsResponse = alpacaCliClient.getOptionContracts(
                symbol,
                minExpiration.toString(),
                maxExpiration.toString(),
                "call",
                100
        );

        List<OptionContract> contracts =
                alpacaCliClient.parseOptionContracts(contractsResponse);

        List<String> optionSymbols = contracts.stream()
                .map(OptionContract::symbol)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<OptionSnapshot> snapshots;

        if (optionSymbols.isEmpty()) {
            snapshots = List.of();
        } else {
            JsonNode quotesResponse =
                    alpacaCliClient.getOptionLatestQuotes(optionSymbols);

            snapshots =
                    alpacaCliClient.parseOptionLatestQuotes(quotesResponse);
        }

        Optional<OptionCandidate> candidate =
                optionSelectionService.select(
                        signal,
                        contracts,
                        snapshots,
                        today
                );

        if (candidate.isEmpty()) {
            return new OptionsDecision(
                    signal,
                    null,
                    null,
                    null,
                    "No valid CALL option candidate available"
            );
        }

        OptionCandidate option = candidate.get();

        LLMConfirmation confirmation =
                optionLLMConfirmationService.confirm(signal, option);

        if (!confirmation.confirmed()) {
            String reason =
                    "LLM rejected the operation: " + confirmation.reasoning();

            decisionHistoryService.record(
                    com.aah.agent.history.DecisionStatus.LLM_REJECTED,
                    reason
            );

            return new OptionsDecision(
                    signal,
                    option,
                    confirmation,
                    RiskResult.rejected(reason),
                    reason
            );
        }

        RiskContext riskContext = buildRiskContext(option);

        RiskResult riskResult =
                riskGateService.evaluate(riskContext, confirmation);

        if (riskResult.allowed()) {
            decisionHistoryService.record(
                    com.aah.agent.history.DecisionStatus.APPROVED,
                    riskResult.reason()
            );
        } else {
            decisionHistoryService.record(
                    com.aah.agent.history.DecisionStatus.RISK_REJECTED,
                    riskResult.reason()
            );
        }

        return new OptionsDecision(
                signal,
                option,
                confirmation,
                riskResult,
                riskResult.reason()
        );
    }

    private RiskContext buildRiskContext(
            OptionCandidate option
    ) throws Exception {

        JsonNode account = alpacaCliClient.getAccount();

        double accountValue = extractAccountValue(account);

        double optionCost = option.ask()
                .multiply(BigDecimal.valueOf(option.multiplier()))
                .doubleValue();

        int dailyOperations =
                (int) decisionHistoryService.countByStatus(
                        com.aah.agent.history.DecisionStatus.APPROVED
                );

        return new RiskContext(
                accountValue,
                optionCost,
                dailyOperations,
                (int) option.daysToExpiration(),
                0.0
        );
    }

    private double extractAccountValue(JsonNode account) {
        JsonNode value = account.get("equity");

        if (value == null || value.isNull()) {
            throw new IllegalStateException(
                    "Alpaca account response does not contain a valid equity value"
            );
        }

        try {
            return value.isNumber()
                    ? value.doubleValue()
                    : Double.parseDouble(value.asText());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Alpaca account response does not contain a valid equity value",
                    exception
            );
        }
    }
}
