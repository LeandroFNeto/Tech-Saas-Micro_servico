export interface JidDecodificado {
  usuario: string;
  servidor: string;
}

export function decodificar(jid: string | null | undefined): JidDecodificado | null {
  if (!jid) {
    return null;
  }
  const arroba = jid.indexOf('@');
  if (arroba < 1) {
    return null;
  }
  const bruto = jid.slice(0, arroba);
  const servidor = jid.slice(arroba + 1);
  const usuario = bruto.split(':')[0].split('_')[0];
  if (usuario === '') {
    return null;
  }
  return { usuario, servidor };
}

export function conversaAceita(jid: string | null | undefined): boolean {
  const d = decodificar(jid);
  return d !== null && (d.servidor === 's.whatsapp.net' || d.servidor === 'lid');
}

export function ehLid(jid: string | null | undefined): boolean {
  return decodificar(jid)?.servidor === 'lid';
}

export function normalizar(jid: string | null | undefined): string | null {
  const d = decodificar(jid);
  if (!d) {
    return null;
  }
  if (d.servidor === 's.whatsapp.net' || d.servidor === 'c.us') {
    return `${d.usuario}@c.us`;
  }
  if (d.servidor === 'lid') {
    return `${d.usuario}@lid`;
  }
  return `${d.usuario}@${d.servidor}`;
}

export function usuarioDe(jid: string | null | undefined): string | null {
  return decodificar(jid)?.usuario ?? null;
}

export type Destino = { tipo: 'lid'; jid: string } | { tipo: 'numero'; digitos: string };

export function montarDestino(phone: string, isLid: boolean): Destino | null {
  const bruto = (phone ?? '').trim();
  const comSufixoLid = bruto.toLowerCase().includes('@lid');
  const digitos = bruto.split('@')[0].replace(/\D/g, '');
  if (digitos.length < 8 || digitos.length > 20) {
    return null;
  }
  if (isLid || comSufixoLid || digitos.length >= 14) {
    return { tipo: 'lid', jid: `${digitos}@lid` };
  }
  return { tipo: 'numero', digitos };
}
