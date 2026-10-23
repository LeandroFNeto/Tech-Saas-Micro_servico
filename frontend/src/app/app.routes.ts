import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },
  {
    path: 'cliente',
    canActivate: [authGuard, roleGuard('CLIENTE')],
    loadChildren: () => import('./features/client/client.routes').then((m) => m.CLIENT_ROUTES)
  },
  {
    path: 'design-v0',
    loadComponent: () =>
      import('./features/design-v0/v0-dashboard/v0-dashboard.component').then((m) => m.V0DashboardComponent)
  },
  { path: '**', redirectTo: 'login' }
];
