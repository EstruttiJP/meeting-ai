import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [Button],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    // Se a sessão do backend ainda estiver válida, não faz sentido mostrar login de novo.
    if (this.authService.status() !== 'authenticated') {
      this.authService.loadCurrentUser().subscribe((user) => {
        if (user) {
          this.router.navigateByUrl('/dashboard');
        }
      });
    }
  }

  protected loginWithGoogle() {
    this.authService.redirectToGoogleLogin();
  }
}
