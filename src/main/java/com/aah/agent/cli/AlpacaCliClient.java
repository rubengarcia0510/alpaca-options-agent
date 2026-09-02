package com.aah.agent.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Invoca el Alpaca CLI oficial (github.com/alpacahq/cli) como subproceso.
 * Esta es la pieza que cumple el requisito del hackathon "MCP or CLI" —
 * el resto de la app (detección de señal, confirmación del LLM, gates de
 * riesgo) es "el cerebro" en Java, pero toda ejecución real contra la
 * cuenta de Alpaca pasa por acá, invocando el binario `alpaca`.
 *
 * El CLI ya está logueado en la máquina (alpaca profile login, hecho una
 * sola vez con OAuth) — esta clase no maneja API keys directamente.
 */
@Component
public class AlpacaCliClient {

    private static final int TIMEOUT_SECONDS = 30;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode run(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("alpaca");
        command.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Comando 'alpaca " + String.join(" ", args) + "' superó el timeout de "
                    + TIMEOUT_SECONDS + "s");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Comando 'alpaca " + String.join(" ", args) + "' falló (exit " + exitCode
                    + "): " + stderr);
        }

        if (stdout.isBlank()) {
            throw new RuntimeException("Comando 'alpaca " + String.join(" ", args) + "' no devolvió salida");
        }

        return objectMapper.readTree(stdout);
    }

    private String readStream(java.io.InputStream input) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    public JsonNode getAccount() throws Exception {
        return run("account", "get");
    }

    public JsonNode listPositions() throws Exception {
        return run("position", "list");
    }

    public JsonNode listOpenOrders() throws Exception {
        return run("order", "list");
    }

    public JsonNode submitMarketOrder(String symbol, String side, int qty) throws Exception {
        return run("order", "submit",
                "--symbol", symbol,
                "--side", side,
                "--qty", String.valueOf(qty),
                "--type", "market");
    }

    // Pendiente confirmar sintaxis exacta con: alpaca data option --help
    public JsonNode getStockBars(String symbol, int limit) throws Exception {
        String start = java.time.LocalDate.now()
                .minusDays(60)
                .toString();

        return run(
                "data", "bars",
                "--symbol", symbol,
                "--start", start,
                "--timeframe", "1Day",
                "--limit", String.valueOf(limit)
        );
    }

    public JsonNode getOptionChain(String underlyingSymbol) throws Exception {
        return run("data", "option", "chain", "--underlying-symbol", underlyingSymbol);
    }
}
