# Alpaca Options Agent — AI-Assisted Trading Decision Engine

## Problem / Goal

Alpaca Options Agent is a cautious AI-assisted trading prototype designed to identify potential bullish CALL option setups while keeping the final decision constrained by predefined deterministic risk controls.

The system separates market analysis, AI confirmation, risk validation, and decision history so that an AI recommendation cannot bypass safety rules.

## AI Decision Flow

Alpaca market data
→ Technical Signal (SMA 9 / SMA 21)
→ LLM Confirmation
→ Deterministic Risk Gates
→ Decision History

If the LLM rejects the setup, the decision is recorded as `LLM_REJECTED`.

If the risk gates reject the setup, it is recorded as `RISK_REJECTED`.

If all checks pass, it is recorded as `APPROVED`.

## Technical Signal

The signal service obtains stock bar data through the Alpaca CLI and calculates short and long simple moving averages.

A bullish setup is generated only when SMA(9) crosses above SMA(21). If insufficient data is available, or no new bullish crossover is detected, no trading signal is produced.

## AI Confirmation

The LLM receives the technical context and evaluates the setup independently.

The prompt requires:

- `CONFIRMED: YES` or `NO`
- `REASONING: ...`

The model is explicitly instructed not to invent market data.

An absent, empty, or negative confirmation is treated as a rejection.

## Deterministic Risk Gates

An LLM confirmation is not sufficient to approve a trade.

The risk layer validates:

- Maximum account risk
- Maximum number of daily operations
- Minimum and maximum days to expiration
- Maximum option loss ratio
- Valid account and option risk data

Any failed gate produces a deterministic rejection reason.

## Decision History

Every evaluated decision is recorded with:

- Timestamp
- Decision status
- Reasoning

Supported statuses:

- `APPROVED`
- `LLM_REJECTED`
- `RISK_REJECTED`

This provides the foundation for future P&L calculation, auditability, and reporting.

## Alpaca Integration

Market data is obtained through the Alpaca CLI integration.

The current prototype focuses on market analysis, AI confirmation, deterministic risk controls, and decision history. Live order execution is not represented as implemented functionality in this document.

## Safety / Human-in-the-Loop

The decision process is deliberately conservative:

1. Market data produces a technical signal.
2. The LLM independently evaluates the setup.
3. Deterministic risk gates can reject the operation.
4. The decision and reasoning are recorded.

The AI therefore acts as an assisted decision component rather than unrestricted trading authority.

## Infrastructure / Implementation

The application is implemented using Java 21 and Spring.

The codebase separates:

- Alpaca market-data access
- Technical signal generation
- LLM confirmation
- Risk validation
- Trade decision orchestration
- Decision history

Automated tests cover the signal, LLM, risk, decision, and decision-history layers.

## Validation

Current validation:

**22 tests — 0 failures — 0 errors — BUILD SUCCESS**

The decision-history feature was integrated into `main` through PR #4.

## Hackathon Submission Summary

Alpaca Options Agent demonstrates how AI can assist options-trading decisions while remaining constrained by deterministic safety rules.

The architecture combines real market data, technical analysis, independent LLM reasoning, explicit risk gates, and decision history in a small, testable Java/Spring implementation.

The design prioritizes explainability, controlled automation, and traceability over unrestricted autonomous trading.
