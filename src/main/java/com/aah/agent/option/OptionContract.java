package com.aah.agent.option;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OptionContract(
        String id,
        String symbol,
        String underlyingSymbol,
        LocalDate expirationDate,
        BigDecimal strikePrice,
        String type,
        String status,
        boolean tradable,
        int multiplier
) {
}
