package com.aah.agent.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionHistoryServiceTest {

    @Test
    void shouldRecordAndReturnDecisionHistory() {
        var service = new DecisionHistoryService();

        var record = service.record(
                DecisionStatus.APPROVED,
                "All risk gates passed"
        );

        var history = service.getHistory();

        assertEquals(1, history.size());
        assertEquals(record, history.get(0));
        assertEquals(DecisionStatus.APPROVED, history.get(0).status());
        assertEquals("All risk gates passed", history.get(0).reasoning());
        assertNotNull(history.get(0).timestamp());
    }

    @Test
    void shouldKeepMultipleDecisionsInOrder() {
        var service = new DecisionHistoryService();

        service.record(
                DecisionStatus.LLM_REJECTED,
                "Weak setup"
        );
        service.record(
                DecisionStatus.RISK_REJECTED,
                "Operation exceeds maximum account risk"
        );
        service.record(
                DecisionStatus.APPROVED,
                "All risk gates passed"
        );

        var history = service.getHistory();

        assertEquals(3, history.size());
        assertEquals(DecisionStatus.LLM_REJECTED, history.get(0).status());
        assertEquals(DecisionStatus.RISK_REJECTED, history.get(1).status());
        assertEquals(DecisionStatus.APPROVED, history.get(2).status());
    }

    @Test
    void shouldReturnUnmodifiableHistory() {
        var service = new DecisionHistoryService();

        service.record(
                DecisionStatus.APPROVED,
                "All risk gates passed"
        );

        var history = service.getHistory();

        assertThrows(
                UnsupportedOperationException.class,
                () -> history.clear()
        );
    }
}
