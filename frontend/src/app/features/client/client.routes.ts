import { Routes } from '@angular/router';
import { ShellComponent } from '../../shared/components/shell/shell.component';

export const CLIENT_ROUTES: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./painel/painel.component').then((m) => m.PainelComponent)
      },
      {
        path: 'chatbot',
        loadComponent: () => import('./perfil/perfil.component').then((m) => m.PerfilComponent)
      }
    ]
  }
];
