package com.aah.agent.web;

import com.aah.agent.execution.ExecutionAuthorization;
import com.aah.agent.execution.ExecutionResult;
import com.aah.agent.execution.OptionOrderExecutionService;
import com.aah.agent.orchestrator.OptionsAgentOrchestrator;
import com.aah.agent.orchestrator.OptionsDecision;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/options")
public class OptionsAgentController {

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
            @RequestParam(defaultValue = "SPY") String symbol
    ) throws Exception {
        return orchestrator.evaluate(symbol);
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
