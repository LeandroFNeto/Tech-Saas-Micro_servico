import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ShellComponent } from '../../shared/components/shell/shell.component';

export const escritaAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLeitura() ? router.createUrlTree(['/admin']) : true;
};

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
        canActivate: [escritaAdminGuard],
        loadComponent: () =>
          import('./empresa-form/empresa-form.component').then((m) => m.EmpresaFormComponent)
      },
      {
        path: 'empresas/:sessao/editar',
        loadComponent: () =>
          import('./empresa-form/empresa-form.component').then((m) => m.EmpresaFormComponent)
      },
      {
        path: 'empresas/:sessao',
        loadComponent: () =>
          import('./empresa-form/empresa-form.component').then((m) => m.EmpresaFormComponent)
      },
      {
        path: 'avisos',
        loadComponent: () =>
          import('./gestao-avisos/gestao-avisos.component').then((m) => m.GestaoAvisosComponent)
      },
      {
        path: 'assistente-tutoria',
        loadComponent: () =>
          import('./laboratorio-testes/laboratorio-testes.component').then((m) => m.LaboratorioTestesComponent)
      }
    ]
  }
];
