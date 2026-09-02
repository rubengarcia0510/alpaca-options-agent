package com.aah.agent.option;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OptionCandidate(
        String contractId,
        String symbol,
        String underlyingSymbol,
        LocalDate expirationDate,
        long daysToExpiration,
        BigDecimal strikePrice,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal entryPrice,
        BigDecimal delta,
        BigDecimal impliedVolatility,
        int multiplier
) {
}
