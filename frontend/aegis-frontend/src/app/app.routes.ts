import { Routes } from '@angular/router';

export const routes: Routes = [
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
    path: 'wallets',
    loadComponent: () => import('./features/wallet/wallet.component')
      .then(m => m.WalletComponent)
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];
