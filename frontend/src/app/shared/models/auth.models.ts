export type PapelUsuario = 'ADMIN' | 'SUPER_ADMIN' | 'ADMIN_OPERACIONAL' | 'ADMIN_LEITURA' | 'CLIENTE';

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
