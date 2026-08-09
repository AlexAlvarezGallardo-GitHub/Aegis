import { Routes } from '@angular/router';
import { AuthGuard } from './shared/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [AuthGuard],
    loadComponent: () => import('./shared/layout/app-shell/app-shell.component')
      .then(m => m.AppShellComponent),
    children: [
      {
        path: '',
        redirectTo: 'wallets',
        pathMatch: 'full',
      },
      {
        path: 'wallets',
        loadComponent: () => import('./features/wallet/wallet.component')
          .then(m => m.WalletComponent),
      },
      {
        path: 'wallets/:walletId',
        loadComponent: () => import('./features/wallet/wallet-detail/wallet-detail.component')
          .then(m => m.WalletDetailComponent),
        data: { title: 'Wallet Detail' },
      },
      {
        path: 'payments',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Payments' },
      },
      {
        path: 'transactions',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Transactions' },
      },
      {
        path: 'payouts',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Payouts' },
      },
      {
        path: 'currencies',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Currencies' },
      },
      {
        path: 'fraud',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Fraud Detection' },
      },
      {
        path: 'alerts',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Alerts' },
      },
      {
        path: 'health',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'System Health' },
      },
      {
        path: 'settings',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Settings' },
      },
      {
        path: 'users',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'Users' },
      },
      {
        path: 'api-keys',
        loadComponent: () => import('./shared/layout/page-placeholder/page-placeholder.component')
          .then(m => m.PagePlaceholderComponent),
        data: { title: 'API Keys' },
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/auth.component')
      .then(m => m.AuthComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/registration/registration.component')
      .then(m => m.RegistrationComponent),
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
