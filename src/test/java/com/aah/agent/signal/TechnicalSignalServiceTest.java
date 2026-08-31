package com.aah.agent.signal;

import com.aah.agent.cli.AlpacaCliClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalSignalServiceTest {

    @Mock
    private AlpacaCliClient alpacaCliClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnNoSignalWhenThereAreNotEnoughBars() throws Exception {
        when(alpacaCliClient.getStockBars("SPY", 22))
                .thenReturn(bars(100, 101, 102, 103, 104));

        TechnicalSignalService service =
                new TechnicalSignalService(alpacaCliClient);

        TechnicalSignal result = service.analyze("SPY");

        assertFalse(result.signal());
        assertTrue(result.reason().contains("Datos insuficientes"));
    }

    @Test
    void shouldReturnNoSignalWhenThereIsNoBullishCross() throws Exception {
        when(alpacaCliClient.getStockBars("SPY", 22))
                .thenReturn(bars(
                        110, 109, 108, 107, 106, 105, 104, 103, 102, 101,
                        100, 99, 98, 97, 96, 95, 94, 93, 92, 91,
                        90, 89, 88
                ));

        TechnicalSignalService service =
                new TechnicalSignalService(alpacaCliClient);

        TechnicalSignal result = service.analyze("SPY");

        assertFalse(result.signal());
    }

    @Test
    void shouldDetectBullishSmaCross() throws Exception {
        when(alpacaCliClient.getStockBars("SPY", 22))
                .thenReturn(bars(
                        100, 99, 98, 97, 96, 95, 94, 93, 92, 91,
                        90, 89, 88, 87, 86, 85, 84, 83, 82, 81,
                        80, 150, 170
                ));

        TechnicalSignalService service =
                new TechnicalSignalService(alpacaCliClient);

        TechnicalSignal result = service.analyze("SPY");

        assertTrue(result.signal());
        assertEquals("SPY", result.symbol());
        assertTrue(result.shortSma() > result.longSma());
        assertEquals(170.0, result.price());
        assertTrue(result.reason().contains("Cruce alcista"));
    }

    @Test
    void shouldNotDetectBearishCrossAsBullishSignal() throws Exception {
        when(alpacaCliClient.getStockBars("SPY", 22))
                .thenReturn(bars(
                        80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
                        90, 91, 92, 93, 94, 95, 96, 97, 98, 99,
                        100, 99, 80
                ));

        TechnicalSignalService service =
                new TechnicalSignalService(alpacaCliClient);

        TechnicalSignal result = service.analyze("SPY");

        assertFalse(result.signal());
    }

    private JsonNode bars(double... closes) {
        var root = objectMapper.createObjectNode();
        var bars = root.putArray("bars");

        Arrays.stream(closes).forEach(close -> {
            var bar = bars.addObject();
            bar.put("c", close);
        });

        return root;
    }
}
