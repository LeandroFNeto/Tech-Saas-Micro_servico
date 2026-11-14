export interface Config {
  porta: number;
  segredo: string;
  segredoWebhook: string;
  urlWebhookPadrao: string;
  hostsWebhookPermitidos: string[];
  aoMensagemDoDono: boolean;
  simularDigitacao: boolean;
  pastaAutenticacao: string;
  nivelLog: string;
  buscarVersaoWhatsapp: boolean;
  hostsFotoPermitidos: string[];
  limiteImagemBytes: number;
  timeoutDownloadMs: number;
}

export class ErroDeConfiguracao extends Error {}

type Ambiente = Record<string, string | undefined>;

function texto(env: Ambiente, nome: string): string {
  return (env[nome] ?? '').trim();
}

function verdadeiro(valor: string, padrao: boolean): boolean {
  if (valor === '') {
    return padrao;
  }
  return valor.toLowerCase() === 'true';
}

function inteiro(valor: string, padrao: number): number {
  const numero = Number.parseInt(valor, 10);
  return Number.isFinite(numero) && numero > 0 ? numero : padrao;
}

export function validarUrlWebhook(valor: string): boolean {
  try {
    const url = new URL(valor);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function webhookPermitido(valor: string, hostsPermitidos: string[]): boolean {
  if (!validarUrlWebhook(valor)) {
    return false;
  }
  return hostsPermitidos.includes(new URL(valor).hostname.toLowerCase());
}

function listaDeHosts(valor: string): string[] {
  return valor
    .split(',')
    .map((host) => host.trim().toLowerCase())
    .filter((host) => host !== '');
}

export function carregarConfig(env: Ambiente = process.env): Config {
  const segredo = texto(env, 'SECRET_KEY');
  if (segredo === '') {
    throw new ErroDeConfiguracao('SECRET_KEY ausente: o gateway nao sobe sem o segredo da API');
  }
  const segredoWebhook = texto(env, 'WPPCONNECT_WEBHOOK_SECRET') || segredo;
  if (segredoWebhook === '') {
    throw new ErroDeConfiguracao('WPPCONNECT_WEBHOOK_SECRET ausente: o gateway nao sobe sem o segredo do webhook');
  }
  const urlWebhookPadrao = texto(env, 'WPPCONNECT_WEBHOOK_URL') || 'http://bot-java:8080/webhook/whatsapp';
  if (!validarUrlWebhook(urlWebhookPadrao)) {
    throw new ErroDeConfiguracao('WPPCONNECT_WEBHOOK_URL invalida');
  }
  return {
    porta: inteiro(texto(env, 'PORT'), 21465),
    segredo,
    segredoWebhook,
    urlWebhookPadrao,
    hostsWebhookPermitidos: [
      ...new Set([new URL(urlWebhookPadrao).hostname.toLowerCase(), ...listaDeHosts(texto(env, 'BAILEYS_HOSTS_WEBHOOK_PERMITIDOS'))])
    ],
    aoMensagemDoDono: verdadeiro(texto(env, 'WPPCONNECT_ON_SELF_MESSAGE'), true),
    simularDigitacao: verdadeiro(texto(env, 'BAILEYS_SIMULAR_DIGITACAO'), true),
    pastaAutenticacao: texto(env, 'BAILEYS_AUTH_DIR') || '/auth-data',
    nivelLog: texto(env, 'LOG_LEVEL') || 'info',
    buscarVersaoWhatsapp: verdadeiro(texto(env, 'BAILEYS_BUSCAR_VERSAO_WA'), false),
    hostsFotoPermitidos: listaDeHosts(texto(env, 'BAILEYS_HOSTS_FOTO_PERMITIDOS')),
    limiteImagemBytes: inteiro(texto(env, 'BAILEYS_LIMITE_IMAGEM_BYTES'), 5 * 1024 * 1024),
    timeoutDownloadMs: inteiro(texto(env, 'BAILEYS_TIMEOUT_DOWNLOAD_MS'), 4000)
  };
}
