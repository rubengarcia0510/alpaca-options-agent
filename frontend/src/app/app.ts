import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { timeout } from 'rxjs';

interface TechnicalSignal {
  symbol: string;
  signal: boolean;
  shortSma: number;
  longSma: number;
  price: number;
  reason: string;
}

interface OptionCandidate {
  contractId: string;
  symbol: string;
  underlyingSymbol: string;
  expirationDate: string;
  daysToExpiration: number;
  strikePrice: number;
  bid: number;
  ask: number;
  entryPrice: number;
  delta: number;
  impliedVolatility: number | null;
  multiplier: number;
}

interface LLMConfirmation {
  confirmed: boolean;
  reasoning: string;
}

interface RiskResult {
  allowed: boolean;
  reason: string;
}

interface OptionsDecision {
  signal: TechnicalSignal;
  optionCandidate: OptionCandidate | null;
  llmConfirmation: LLMConfirmation | null;
  riskResult: RiskResult | null;
  reason: string;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
private readonly apiUrl = 'https://alpaca-options-agent.onrender.com';
  constructor(private readonly http: HttpClient) {}

  symbol = 'SPY';
  evaluating = false;
  evaluated = false;
  decision: OptionsDecision | null = null;
  errorMessage = '';

  evaluate(): void {
    this.evaluating = true;
    this.evaluated = false;
    this.decision = null;
    this.errorMessage = '';

    this.http
      .get<OptionsDecision>(`${this.apiUrl}/api/options/evaluate`, {
        params: { symbol: this.symbol.trim().toUpperCase() }
      })
      .pipe(timeout(60000))
      .subscribe({
        next: (decision) => {
          this.evaluating = false;
          this.evaluated = true;
          this.decision = decision;
        },
        error: (error) => {
          console.error('Options evaluation failed', error);
          this.evaluating = false;
          this.evaluated = false;
          this.errorMessage =
            error?.name === 'TimeoutError'
              ? 'Evaluation timed out. Please try again.'
              : 'Unable to evaluate the selected symbol.';
        }
      });
  }
}
