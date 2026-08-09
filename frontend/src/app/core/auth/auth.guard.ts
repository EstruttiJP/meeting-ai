import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth.service';

// Cada navegação guardada reconsulta /api/users/me em vez de confiar num signal
// que pode estar desatualizado (ex.: sessão expirada no backend entre navegações).
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.status() === 'authenticated') {
    return true;
  }

  return authService.loadCurrentUser().pipe(map((user) => (user ? true : router.parseUrl('/login'))));
};
