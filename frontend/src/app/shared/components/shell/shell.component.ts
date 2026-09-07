import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html'
})
export class ShellComponent {
  private readonly authService = inject(AuthService);

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
