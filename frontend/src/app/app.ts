import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';

interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

const BACKEND_HEALTH_URL = 'http://localhost:8080/api/health';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly http = inject(HttpClient);

  protected readonly title = signal('Meeting AI');
  protected readonly backendStatus = signal<'checking' | 'up' | 'down'>('checking');
  protected readonly backendService = signal<string | null>(null);

  constructor() {
    this.http
      .get<HealthResponse>(BACKEND_HEALTH_URL)
      .pipe(catchError(() => of(null)))
      .subscribe((response) => {
        if (response?.status === 'UP') {
          this.backendStatus.set('up');
          this.backendService.set(response.service);
        } else {
          this.backendStatus.set('down');
        }
      });
  }
}
