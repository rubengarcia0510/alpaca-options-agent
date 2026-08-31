package com.aah.agent.history;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionHistoryService {

    private final List<DecisionRecord> history = new ArrayList<>();

    public DecisionRecord record(
            DecisionStatus status,
            String reasoning
    ) {
        DecisionRecord record = new DecisionRecord(
                Instant.now(),
                status,
                reasoning
        );

        history.add(record);
        return record;
    }

    public List<DecisionRecord> getHistory() {
        return List.copyOf(history);
    }
}
