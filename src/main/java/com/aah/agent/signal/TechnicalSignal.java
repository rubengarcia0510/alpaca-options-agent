package com.aah.agent.signal;

public record TechnicalSignal(
        String symbol,
        boolean signal,
        double shortSma,
        double longSma,
        double price,
        String reason
) {

    public static TechnicalSignal signal(
            String symbol,
            double shortSma,
            double longSma,
            double price,
            String reason) {

        return new TechnicalSignal(
                symbol,
                true,
                shortSma,
                longSma,
                price,
                reason
        );
    }

    public static TechnicalSignal noSignal(
            String symbol,
            String reason) {

        return new TechnicalSignal(
                symbol,
                false,
                0.0,
                0.0,
                0.0,
                reason
        );
    }
}
