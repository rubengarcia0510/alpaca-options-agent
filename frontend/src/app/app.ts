import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly apiUrl = 'http://localhost:8080';

  constructor(private readonly http: HttpClient) {}
  symbol = 'SPY';
  evaluating = false;
  evaluated = false;

  evaluate(): void {
    this.evaluating = true;
    this.evaluated = false;

    this.http
      .get(`${this.apiUrl}/api/options/evaluate`, {
        params: { symbol: this.symbol }
      })
      .subscribe({
        next: () => {
          this.evaluating = false;
          this.evaluated = true;
        },
        error: (error) => {
          console.error('Options evaluation failed', error);
          this.evaluating = false;
          this.evaluated = false;
        }
      });
  }
}
