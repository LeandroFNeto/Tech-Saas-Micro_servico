import { createHash, createHmac, timingSafeEqual } from 'node:crypto';

export const PADRAO_SESSAO = /^[A-Za-z0-9_.-]{1,100}$/;

export function sessaoValida(sessao: string): boolean {
  return PADRAO_SESSAO.test(sessao) && sessao !== '.' && sessao !== '..';
}

export function igualSeguro(a: string, b: string): boolean {
  const ha = createHash('sha256').update(a, 'utf8').digest();
  const hb = createHash('sha256').update(b, 'utf8').digest();
  return timingSafeEqual(ha, hb);
}

export function gerarToken(segredo: string, sessao: string): string {
  return createHmac('sha256', segredo).update(sessao, 'utf8').digest('hex');
}

export function tokenValido(segredo: string, sessao: string, token: string | undefined): boolean {
  if (!token) {
    return false;
  }
  return igualSeguro(gerarToken(segredo, sessao), token);
}

export function extrairBearer(cabecalho: string | undefined): string | undefined {
  if (!cabecalho) {
    return undefined;
  }
  const partes = cabecalho.split(' ');
  if (partes.length !== 2 || partes[0].toLowerCase() !== 'bearer' || partes[1] === '') {
    return undefined;
  }
  return partes[1];
}
