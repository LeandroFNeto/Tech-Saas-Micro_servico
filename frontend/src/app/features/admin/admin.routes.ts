import { Routes } from '@angular/router';
import { ShellComponent } from '../../shared/components/shell/shell.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'empresas/nova',
        loadComponent: () =>
          import('./empresa-form/empresa-form.component').then((m) => m.EmpresaFormComponent)
      },
      {
        path: 'empresas/:sessao',
        loadComponent: () =>
          import('./empresa-form/empresa-form.component').then((m) => m.EmpresaFormComponent)
      }
    ]
  }
];
