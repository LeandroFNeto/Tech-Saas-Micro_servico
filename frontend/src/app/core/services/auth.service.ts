import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AlterarSenhaRequest,
  LoginRequest,
  LoginResponse,
  PapelUsuario,
  SessaoUsuario
} from '../../shared/models/auth.models';

const CHAVE_JWT = 'gamb.jwt';
const CHAVE_SESSAO = 'gamb.sessao';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly sessao = signal<SessaoUsuario | null>(this.lerSessao());

  constructor(
    private readonly router: Router,
    private readonly http: HttpClient
  ) {}

  token(): string | null {
    return localStorage.getItem(CHAVE_JWT);
  }

  isAuthenticated(): boolean {
    const atual = this.sessao();
    return !!atual && atual.exp > Date.now();
  }

  possuiPapel(papel: PapelUsuario): boolean {
    return this.isAuthenticated() && this.sessao()?.papel === papel;
  }

  rotaInicial(): string {
    return this.sessao()?.papel === 'ADMIN' ? '/admin' : '/cliente';
  }

  login(credenciais: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credenciais).pipe(
      tap((resposta) => this.persistir(resposta))
    );
  }

  alterarSenha(dto: AlterarSenhaRequest): Observable<void> {
    return this.http.put(`${environment.apiUrl}/auth/alterar-senha`, dto, { responseType: 'text' }).pipe(
      map(() => undefined)
    );
  }

  logout(): void {
    localStorage.removeItem(CHAVE_JWT);
    localStorage.removeItem(CHAVE_SESSAO);
    this.sessao.set(null);
    void this.router.navigateByUrl('/login');
  }

  private persistir(resposta: LoginResponse): void {
    localStorage.setItem(CHAVE_JWT, resposta.token);
    const claims = this.decodificarJwt(resposta.token);
    const payload: SessaoUsuario = {
      sub: resposta.email,
      nome: resposta.nome ?? String(claims?.['nome'] ?? resposta.email),
      papel: resposta.role,
      sessaoWhatsapp: resposta.sessaoWhatsapp ?? (claims?.['sessaoWhatsapp'] as string | undefined),
      exp: typeof claims?.['exp'] === 'number' ? claims['exp'] * 1000 : Date.now() + 8 * 60 * 60 * 1000
    };
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(payload));
    this.sessao.set(payload);
  }

  private decodificarJwt(token: string): Record<string, unknown> | null {
    try {
      const parte = token.split('.')[1];
      if (!parte) {
        return null;
      }
      const json = atob(parte.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private lerSessao(): SessaoUsuario | null {
    const bruto = localStorage.getItem(CHAVE_SESSAO);
    if (!bruto) {
      return null;
    }

    try {
      const parsed = JSON.parse(bruto) as SessaoUsuario;
      if (parsed.exp <= Date.now()) {
        localStorage.removeItem(CHAVE_JWT);
        localStorage.removeItem(CHAVE_SESSAO);
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }
}
