export type PapelUsuario = 'ADMIN' | 'CLIENTE';

export interface SessaoUsuario {
  sub: string;
  nome: string;
  papel: PapelUsuario;
  sessaoWhatsapp?: string;
  exp: number;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: PapelUsuario;
  nome?: string;
  sessaoWhatsapp?: string;
}

export interface AlterarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

/** @deprecated Use LoginRequest. Mantido para compatibilidade. */
export type CredenciaisLogin = LoginRequest;
