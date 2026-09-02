package com.aah.agent.option;

import com.aah.agent.risk.RiskGateProperties;
import com.aah.agent.signal.TechnicalSignal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OptionSelectionService {

    private static final long TARGET_DTE = 21;

    private final RiskGateProperties riskGateProperties;

    public OptionSelectionService(RiskGateProperties riskGateProperties) {
        this.riskGateProperties = riskGateProperties;
    }

    public Optional<OptionCandidate> select(
            TechnicalSignal signal,
            List<OptionContract> contracts,
            List<OptionSnapshot> snapshots,
            LocalDate asOfDate) {

        if (signal == null || !signal.signal() || signal.price() <= 0) {
            return Optional.empty();
        }

        Map<String, OptionSnapshot> snapshotsBySymbol = snapshots.stream()
                .filter(snapshot -> snapshot.symbol() != null)
                .collect(Collectors.toMap(
                        OptionSnapshot::symbol,
                        Function.identity(),
                        (first, second) -> first
                ));

        BigDecimal underlyingPrice = BigDecimal.valueOf(signal.price());

        return contracts.stream()
                .filter(this::isEligibleContract)
                .map(contract -> toCandidate(contract, snapshotsBySymbol.get(contract.symbol()),
                        underlyingPrice, asOfDate))
                .flatMap(Optional::stream)
                .min(candidateComparator(underlyingPrice));
    }

    private boolean isEligibleContract(OptionContract contract) {
        return contract != null
                && contract.symbol() != null
                && contract.expirationDate() != null
                && "call".equalsIgnoreCase(contract.type())
                && "active".equalsIgnoreCase(contract.status())
                && contract.tradable()
                && contract.multiplier() > 0;
    }

    private Optional<OptionCandidate> toCandidate(
            OptionContract contract,
            OptionSnapshot snapshot,
            BigDecimal underlyingPrice,
            LocalDate asOfDate) {

        if (snapshot == null
                || snapshot.bid() == null
                || snapshot.ask() == null
                || snapshot.bid().signum() <= 0
                || snapshot.ask().signum() <= 0
                || snapshot.ask().compareTo(snapshot.bid()) < 0
                || contract.strikePrice() == null) {
            return Optional.empty();
        }

        long dte = asOfDate.until(contract.expirationDate()).getDays();

        if (dte < riskGateProperties.minDaysToExpiration()
                || dte > riskGateProperties.maxDaysToExpiration()) {
            return Optional.empty();
        }

        return Optional.of(new OptionCandidate(
                contract.id(),
                contract.symbol(),
                contract.underlyingSymbol(),
                contract.expirationDate(),
                dte,
                contract.strikePrice(),
                snapshot.bid(),
                snapshot.ask(),
                snapshot.ask(),
                snapshot.delta(),
                snapshot.impliedVolatility(),
                contract.multiplier()
        ));
    }

    private Comparator<OptionCandidate> candidateComparator(BigDecimal underlyingPrice) {
        return Comparator
                .comparingLong((OptionCandidate candidate) ->
                        Math.abs(candidate.daysToExpiration() - TARGET_DTE))
                .thenComparingLong(OptionCandidate::daysToExpiration)
                .thenComparing(candidate ->
                        candidate.strikePrice().subtract(underlyingPrice).abs())
                .thenComparing(OptionCandidate::strikePrice)
                .thenComparing(OptionCandidate::symbol);
    }
}
