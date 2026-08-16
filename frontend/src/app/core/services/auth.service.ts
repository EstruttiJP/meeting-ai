import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export type AuthStatus = 'checking' | 'authenticated' | 'anonymous';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<User | null>(null);
  private readonly statusSignal = signal<AuthStatus>('checking');

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly status = this.statusSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.statusSignal() === 'authenticated');

  readonly googleLoginUrl = `${environment.apiBaseUrl}/oauth2/authorization/google`;

  // Verifica a sessão atual perguntando ao backend quem está logado — não há
  // endpoint de "check auth" separado, /api/users/me já serve pra isso (401 = anônimo).
  loadCurrentUser() {
    this.statusSignal.set('checking');
    return this.http.get<User>(`${environment.apiBaseUrl}/api/users/me`).pipe(
      tap((user) => {
        this.currentUserSignal.set(user);
        this.statusSignal.set('authenticated');
      }),
      catchError(() => {
        this.currentUserSignal.set(null);
        this.statusSignal.set('anonymous');
        return of(null);
      }),
    );
  }

  redirectToGoogleLogin() {
    window.location.href = this.googleLoginUrl;
  }

  // Spring Security expõe /logout por padrão (LogoutFilter automático mesmo sem
  // .formLogin() configurado). Recarrega a página depois pra garantir que todo
  // estado em memória (signals, guards) reflita a sessão encerrada.
  logout() {
    return this.http.post(`${environment.apiBaseUrl}/logout`, {}, { responseType: 'text' }).pipe(
      catchError(() => of(null)),
      finalize(() => window.location.assign('/login')),
    );
  }
}
