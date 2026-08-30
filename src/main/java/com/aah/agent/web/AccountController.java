package com.aah.agent.web;

import com.aah.agent.cli.AlpacaCliClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de prueba manual — confirma que AlpacaCliClient invoca bien el
 * CLI oficial de Alpaca de punta a punta, antes de sumar el resto de la
 * lógica del agente (SMA, LLM, gates).
 */
@RestController
public class AccountController {

    private final AlpacaCliClient alpacaCliClient;

    public AccountController(AlpacaCliClient alpacaCliClient) {
        this.alpacaCliClient = alpacaCliClient;
    }

    @GetMapping("/account")
    public JsonNode account() throws Exception {
        return alpacaCliClient.getAccount();
    }
}
