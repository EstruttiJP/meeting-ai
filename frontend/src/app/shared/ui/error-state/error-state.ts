import { Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';

@Component({
  selector: 'app-error-state',
  imports: [Message, Button],
  templateUrl: './error-state.html',
  styleUrl: './error-state.css',
})
export class ErrorState {
  readonly message = input.required<string>();
  readonly retryLabel = input('Tentar novamente');
  readonly retry = output<void>();
}
