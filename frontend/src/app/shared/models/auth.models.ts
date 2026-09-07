export type PapelUsuario = 'ADMIN' | 'CLIENTE';

export interface SessaoUsuario {
  sub: string;
  nome: string;
  papel: PapelUsuario;
  sessaoWhatsapp?: string;
  exp: number;
}

export interface CredenciaisLogin {
  email: string;
  senha: string;
}
