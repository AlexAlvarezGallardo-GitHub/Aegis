import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'register',
    loadComponent: () => import('./features/registration/registration.component')
      .then(m => m.RegistrationComponent)
  },
  {
    path: '',
    redirectTo: 'register',
    pathMatch: 'full'
  }
];
