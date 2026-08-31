package com.aah.agent.signal;

import com.aah.agent.cli.AlpacaCliClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TechnicalSignalService {

    private static final int SHORT_PERIOD = 9;
    private static final int LONG_PERIOD = 21;

    private final AlpacaCliClient alpacaCliClient;

    public TechnicalSignalService(AlpacaCliClient alpacaCliClient) {
        this.alpacaCliClient = alpacaCliClient;
    }

    /**
     * Busca un cruce alcista SMA(9) / SMA(21) en el subyacente.
     *
     * AAH-4:
     * - Obtiene datos mediante Alpaca CLI.
     * - Calcula SMA corta y larga.
     * - Genera señal únicamente ante un cruce alcista nuevo.
     *
     * No ejecuta ninguna orden.
     */
    public TechnicalSignal analyze(String symbol) throws Exception {
        JsonNode response = alpacaCliClient.getStockBars(symbol, LONG_PERIOD + 1);

        List<Double> closes = extractCloses(response);

        if (closes.size() < LONG_PERIOD + 1) {
            return TechnicalSignal.noSignal(
                    symbol,
                    "Datos insuficientes para calcular SMA(9) y SMA(21)"
            );
        }

        int last = closes.size() - 1;
        int previous = last - 1;

        double shortNow = sma(closes, last, SHORT_PERIOD);
        double longNow = sma(closes, last, LONG_PERIOD);

        double shortPrevious = sma(closes, previous, SHORT_PERIOD);
        double longPrevious = sma(closes, previous, LONG_PERIOD);

        boolean crossedUp =
                shortPrevious <= longPrevious &&
                shortNow > longNow;

        if (!crossedUp) {
            return TechnicalSignal.noSignal(
                    symbol,
                    "No se detectó cruce alcista SMA(9)/SMA(21)"
            );
        }

        return TechnicalSignal.signal(
                symbol,
                shortNow,
                longNow,
                closes.get(last),
                "Cruce alcista SMA(9) sobre SMA(21)"
        );
    }

    private double sma(List<Double> values, int endIndex, int period) {
        int start = endIndex - period + 1;

        if (start < 0) {
            throw new IllegalArgumentException("Datos insuficientes para SMA");
        }

        double sum = 0.0;

        for (int i = start; i <= endIndex; i++) {
            sum += values.get(i);
        }

        return sum / period;
    }

    private List<Double> extractCloses(JsonNode response) {
        List<Double> closes = new ArrayList<>();

        JsonNode bars = response.path("bars");

        if (!bars.isArray()) {
            return closes;
        }

        for (JsonNode bar : bars) {
            JsonNode close = bar.get("c");

            if (close != null && close.isNumber()) {
                closes.add(close.asDouble());
            }
        }

        return closes;
    }
}
