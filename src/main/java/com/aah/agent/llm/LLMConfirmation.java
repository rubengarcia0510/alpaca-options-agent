package com.aah.agent.llm;

public record LLMConfirmation(
        boolean confirmed,
        String reasoning
) {
    public static LLMConfirmation confirmed(String reasoning) {
        return new LLMConfirmation(true, reasoning);
    }

    public static LLMConfirmation rejected(String reasoning) {
        return new LLMConfirmation(false, reasoning);
    }
}
