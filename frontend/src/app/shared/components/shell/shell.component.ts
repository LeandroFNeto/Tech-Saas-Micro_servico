import { NgTemplateOutlet } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

const FOCO = 'focus-visible:outline focus-visible:outline-2 focus-visible:outline-accent-400';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgTemplateOutlet],
  templateUrl: './shell.component.html'
})
export class ShellComponent {
  private readonly authService = inject(AuthService);

  /** Classes do item da barra lateral (desktop); o ativo ganha fundo laranja translúcido. */
  readonly linkLateral = `flex min-h-11 items-center gap-2.5 rounded-xl px-3 text-sm font-medium text-brand-100 transition hover:bg-white/[0.08] focus-visible:outline-offset-2 ${FOCO}`;

  /** Classes da aba inferior (celular, só admin); a ativa ganha texto accent-800 e filete laranja no topo. */
  readonly abaInferior = `flex min-h-[60px] flex-1 flex-col items-center justify-center gap-0.5 text-xs font-semibold text-brand-600 focus-visible:-outline-offset-2 ${FOCO}`;

  get admin(): boolean {
    return this.authService.sessao()?.papel === 'ADMIN';
  }

  get nomeUsuario(): string {
    return this.authService.sessao()?.nome ?? '';
  }

  get emailUsuario(): string {
    return this.authService.sessao()?.sub ?? '';
  }

  sair(): void {
    this.authService.logout();
  }
}
