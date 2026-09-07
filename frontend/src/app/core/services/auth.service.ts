import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CredenciaisLogin, PapelUsuario, SessaoUsuario } from '../../shared/models/auth.models';

const CHAVE_JWT = 'gamb.jwt';
const CHAVE_SESSAO = 'gamb.sessao';

interface UsuarioDemo {
  email: string;
  senha: string;
  nome: string;
  papel: PapelUsuario;
  sessaoWhatsapp?: string;
}

const USUARIOS_DEMO: UsuarioDemo[] = [
  {
    email: 'admin@gamb.com',
    senha: 'admin123',
    nome: 'Administrador Master',
    papel: 'ADMIN'
  },
  {
    email: 'cliente@gamb.com',
    senha: 'cliente123',
    nome: 'Recanto Vista Alegre',
    papel: 'CLIENTE',
    sessaoWhatsapp: 'RecantoBot'
  }
];

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly sessao = signal<SessaoUsuario | null>(this.lerSessao());

  constructor(private readonly router: Router) {}

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

  login(credenciais: CredenciaisLogin): void {
    const email = credenciais.email.trim().toLowerCase();
    const encontrado = USUARIOS_DEMO.find((u) => u.email === email);

    if (!encontrado) {
      throw new Error('Usuário não encontrado');
    }

    if (encontrado.senha !== credenciais.senha) {
      throw new Error('Senha incorreta');
    }

    const payload: SessaoUsuario = {
      sub: encontrado.email,
      nome: encontrado.nome,
      papel: encontrado.papel,
      sessaoWhatsapp: encontrado.sessaoWhatsapp,
      exp: Date.now() + 8 * 60 * 60 * 1000
    };

    const jwt = this.montarJwt(payload);
    localStorage.setItem(CHAVE_JWT, jwt);
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(payload));
    this.sessao.set(payload);
  }

  logout(): void {
    localStorage.removeItem(CHAVE_JWT);
    localStorage.removeItem(CHAVE_SESSAO);
    this.sessao.set(null);
    void this.router.navigateByUrl('/login');
  }

  private montarJwt(payload: SessaoUsuario): string {
    const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.assinatura-simulada`;
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
