package com.aah.agent.option;

import java.math.BigDecimal;

public record OptionSnapshot(
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal delta,
        BigDecimal impliedVolatility
) {
}
