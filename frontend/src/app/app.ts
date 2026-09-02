import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  symbol = 'SPY';
  evaluating = false;
  evaluated = false;

  evaluate(): void {
    this.evaluating = true;

    setTimeout(() => {
      this.evaluating = false;
      this.evaluated = true;
    }, 800);
  }
}
