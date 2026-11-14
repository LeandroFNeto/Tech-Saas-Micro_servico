const OCULTO = '***';

export function mascararNumero(valor: string | null | undefined): string {
  if (!valor) {
    return OCULTO;
  }
  const arroba = valor.indexOf('@');
  const parte = arroba >= 0 ? valor.slice(0, arroba) : valor;
  const digitos = parte.replace(/\D/g, '');
  if (digitos.length <= 4) {
    return OCULTO;
  }
  const sufixo = digitos.slice(-4);
  return valor.includes('@lid') ? `lid:${OCULTO}${sufixo}` : `${OCULTO}${sufixo}`;
}

export function tamanhoTexto(texto: string | null | undefined): string {
  return `${texto ? texto.length : 0} caracteres`;
}

const CHAVES_SIGILOSAS = new Set([
  'conversation', 'text', 'body', 'caption', 'message', 'token', 'secret', 'authorization',
  'qr', 'qrcode', 'pushname', 'notifyname', 'phone', 'code', 'creds', 'keys',
  'webhooksecret', 'x-webhook-token', 'password', 'senha', 'apikey'
]);

function mascararTextoLivre(texto: string): string {
  return texto
    .replace(/\b\d{1,20}(?::\d+)?@(?:s\.whatsapp\.net|c\.us|lid|g\.us)\b/g, (jid) => mascararNumero(jid))
    .replace(/\b\d{8,}\b/g, (numero) => `${OCULTO}${numero.slice(-4)}`);
}

export function sanear(valor: unknown, profundidade = 0): unknown {
  if (valor === null || valor === undefined) {
    return valor;
  }
  if (typeof valor === 'string') {
    return mascararTextoLivre(valor);
  }
  if (typeof valor === 'number' || typeof valor === 'boolean') {
    return valor;
  }
  if (valor instanceof Error) {
    return { tipo: valor.name, mensagem: mascararTextoLivre(valor.message) };
  }
  if (Buffer.isBuffer(valor) || valor instanceof Uint8Array) {
    return '[binario]';
  }
  if (profundidade >= 4) {
    return '[...]';
  }
  if (Array.isArray(valor)) {
    return valor.slice(0, 20).map((item) => sanear(item, profundidade + 1));
  }
  if (typeof valor === 'object') {
    const saida: Record<string, unknown> = {};
    for (const [chave, item] of Object.entries(valor as Record<string, unknown>)) {
      saida[chave] = CHAVES_SIGILOSAS.has(chave.toLowerCase()) ? '[oculto]' : sanear(item, profundidade + 1);
    }
    return saida;
  }
  return String(valor);
}
