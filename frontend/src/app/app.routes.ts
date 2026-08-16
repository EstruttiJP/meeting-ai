import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { AppShell } from './shared/ui/app-shell/app-shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'upload',
        loadComponent: () => import('./upload/upload').then((m) => m.Upload),
      },
      {
        path: 'meetings/:id/progress',
        loadComponent: () => import('./progress/progress').then((m) => m.Progress),
      },
      {
        path: 'meetings/:id/review',
        loadComponent: () => import('./review/review').then((m) => m.Review),
      },
      {
        path: 'settings',
        loadComponent: () => import('./settings/settings').then((m) => m.Settings),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
