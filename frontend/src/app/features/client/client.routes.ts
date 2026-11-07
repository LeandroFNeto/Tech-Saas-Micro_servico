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
        path: 'contatos',
        loadComponent: () => import('./contatos/contatos.component').then((m) => m.ContatosComponent)
      },
      {
        path: 'chatbot',
        loadComponent: () => import('./perfil/perfil.component').then((m) => m.PerfilComponent)
      },
      {
        path: 'automacao',
        loadComponent: () => import('./automacao/automacao.component').then((m) => m.AutomacaoComponent)
      },
      {
        path: 'tutoriais',
        loadComponent: () => import('./tutoriais/tutoriais.component').then((m) => m.TutoriaisComponent)
      }
    ]
  }
];
