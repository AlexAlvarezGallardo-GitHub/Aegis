import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./shared/layout/app-shell/app-shell.component')
      .then(m => m.AppShellComponent),
    children: [
      {
        path: 'wallets',
        loadComponent: () => import('./features/wallet/wallet.component')
          .then(m => m.WalletComponent)
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/wallet/wallet.component')
          .then(m => m.WalletComponent)
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/auth.component')
      .then(m => m.AuthComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/registration/registration.component')
      .then(m => m.RegistrationComponent)
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
