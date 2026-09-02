package com.aah.agent.option;

import com.aah.agent.risk.RiskGateProperties;
import com.aah.agent.signal.TechnicalSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionSelectionServiceTest {

    private OptionSelectionService service;

    @BeforeEach
    void setUp() {
        RiskGateProperties risk = new RiskGateProperties(
                0.02,
                5,
                14,
                28,
                0.30
        );

        service = new OptionSelectionService(risk);
    }

    @Test
    void shouldSelectCallClosestToTargetDteAndUnderlyingPrice() {
        LocalDate asOf = LocalDate.of(2026, 9, 1);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                700.0,
                699.0,
                725.0,
                "SMA crossover"
        );

        OptionContract fartherExpiration = contract(
                "SPY260915C00720000",
                LocalDate.of(2026, 9, 15),
                "720"
        );

        OptionContract targetExpirationLowerStrike = contract(
                "SPY260922C00720000",
                LocalDate.of(2026, 9, 22),
                "720"
        );

        OptionContract targetExpirationCloserStrike = contract(
                "SPY260922C00725000",
                LocalDate.of(2026, 9, 22),
                "725"
        );

        List<OptionContract> contracts = List.of(
                fartherExpiration,
                targetExpirationLowerStrike,
                targetExpirationCloserStrike
        );

        List<OptionSnapshot> snapshots = List.of(
                snapshot(fartherExpiration.symbol(), "4.00", "5.00"),
                snapshot(targetExpirationLowerStrike.symbol(), "7.00", "8.00"),
                snapshot(targetExpirationCloserStrike.symbol(), "9.00", "10.00")
        );

        Optional<OptionCandidate> result =
                service.select(signal, contracts, snapshots, asOf);

        assertTrue(result.isPresent());
        assertEquals("SPY260922C00725000", result.get().symbol());
        assertEquals(21, result.get().daysToExpiration());
        assertEquals(new BigDecimal("725"), result.get().strikePrice());
        assertEquals(new BigDecimal("10.00"), result.get().entryPrice());
    }

    @Test
    void shouldRejectContractOutsideDteRange() {
        LocalDate asOf = LocalDate.of(2026, 9, 1);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                700.0,
                699.0,
                725.0,
                "SMA crossover"
        );

        OptionContract contract = contract(
                "SPY260910C00725000",
                LocalDate.of(2026, 9, 10),
                "725"
        );

        Optional<OptionCandidate> result = service.select(
                signal,
                List.of(contract),
                List.of(snapshot(contract.symbol(), "4.00", "5.00")),
                asOf
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNonTradableContract() {
        LocalDate asOf = LocalDate.of(2026, 9, 1);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                700.0,
                699.0,
                725.0,
                "SMA crossover"
        );

        OptionContract contract = new OptionContract(
                "id",
                "SPY260922C00725000",
                "SPY",
                LocalDate.of(2026, 9, 22),
                new BigDecimal("725"),
                "call",
                "active",
                false,
                100
        );

        Optional<OptionCandidate> result = service.select(
                signal,
                List.of(contract),
                List.of(snapshot(contract.symbol(), "4.00", "5.00")),
                asOf
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectMissingOrInvalidQuote() {
        LocalDate asOf = LocalDate.of(2026, 9, 1);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                700.0,
                699.0,
                725.0,
                "SMA crossover"
        );

        OptionContract contract = contract(
                "SPY260922C00725000",
                LocalDate.of(2026, 9, 22),
                "725"
        );

        assertTrue(service.select(
                signal,
                List.of(contract),
                List.of(),
                asOf
        ).isEmpty());

        assertTrue(service.select(
                signal,
                List.of(contract),
                List.of(snapshot(contract.symbol(), "0", "5.00")),
                asOf
        ).isEmpty());

        assertTrue(service.select(
                signal,
                List.of(contract),
                List.of(snapshot(contract.symbol(), "6.00", "5.00")),
                asOf
        ).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoTechnicalSignal() {
        TechnicalSignal signal = TechnicalSignal.noSignal(
                "SPY",
                "No SMA crossover"
        );

        OptionContract contract = contract(
                "SPY260922C00725000",
                LocalDate.of(2026, 9, 22),
                "725"
        );

        Optional<OptionCandidate> result = service.select(
                signal,
                List.of(contract),
                List.of(snapshot(contract.symbol(), "9.00", "10.00")),
                LocalDate.of(2026, 9, 1)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldUseLowerStrikeAsTieBreaker() {
        LocalDate asOf = LocalDate.of(2026, 9, 1);

        TechnicalSignal signal = TechnicalSignal.signal(
                "SPY",
                700.0,
                699.0,
                722.5,
                "SMA crossover"
        );

        OptionContract lower = contract(
                "SPY260922C00720000",
                LocalDate.of(2026, 9, 22),
                "720"
        );

        OptionContract higher = contract(
                "SPY260922C00725000",
                LocalDate.of(2026, 9, 22),
                "725"
        );

        Optional<OptionCandidate> result = service.select(
                signal,
                List.of(higher, lower),
                List.of(
                        snapshot(higher.symbol(), "9.00", "10.00"),
                        snapshot(lower.symbol(), "8.00", "9.00")
                ),
                asOf
        );

        assertTrue(result.isPresent());
        assertEquals(lower.symbol(), result.get().symbol());
    }

    private OptionContract contract(
            String symbol,
            LocalDate expiration,
            String strike) {

        return new OptionContract(
                "id-" + symbol,
                symbol,
                "SPY",
                expiration,
                new BigDecimal(strike),
                "call",
                "active",
                true,
                100
        );
    }

    private OptionSnapshot snapshot(
            String symbol,
            String bid,
            String ask) {

        return new OptionSnapshot(
                symbol,
                new BigDecimal(bid),
                new BigDecimal(ask),
                new BigDecimal("0.80"),
                new BigDecimal("0.20")
        );
    }
}
