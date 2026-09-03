package com.aah.agent.web;

import com.aah.agent.execution.ExecutionAuthorization;
import com.aah.agent.execution.ExecutionResult;
import com.aah.agent.execution.OptionOrderExecutionService;
import com.aah.agent.orchestrator.OptionsAgentOrchestrator;
import com.aah.agent.orchestrator.OptionsDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/options")
public class OptionsAgentController {

    private static final Logger log = LoggerFactory.getLogger(OptionsAgentController.class);

    private final OptionsAgentOrchestrator orchestrator;
    private final OptionOrderExecutionService executionService;

    public OptionsAgentController(
            OptionsAgentOrchestrator orchestrator,
            OptionOrderExecutionService executionService
    ) {
        this.orchestrator = orchestrator;
        this.executionService = executionService;
    }

    @GetMapping("/evaluate")
    public OptionsDecision evaluate(
            @RequestParam(defaultValue = "SPY") String symbol,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId
    ) throws Exception {
        long start = System.nanoTime();
        String normalizedSymbol = symbol.trim().toUpperCase();

        log.info("Options evaluation started: requestId={}, symbol={}", requestId, normalizedSymbol);

        try {
            OptionsDecision decision = orchestrator.evaluate(normalizedSymbol);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            log.info(
                    "Options evaluation completed: requestId={}, symbol={}, signal={}, optionSelected={}, llmConfirmed={}, riskAllowed={}, durationMs={}",
                    requestId,
                    normalizedSymbol,
                    decision.signal() != null && decision.signal().signal(),
                    decision.optionCandidate() != null,
                    decision.llmConfirmation() != null && decision.llmConfirmation().confirmed(),
                    decision.riskResult() != null && decision.riskResult().allowed(),
                    durationMs
            );

            return decision;
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.error(
                    "Options evaluation failed: requestId={}, symbol={}, durationMs={}, error={}",
                    requestId,
                    normalizedSymbol,
                    durationMs,
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    @PostMapping("/execute")
    public ExecutionResult execute(
            @RequestBody ExecuteRequest request
    ) {
        return executionService.execute(
                request.decision(),
                request.authorization(),
                request.quantity()
        );
    }

    public record ExecuteRequest(
            OptionsDecision decision,
            ExecutionAuthorization authorization,
            int quantity
    ) {
    }
}
