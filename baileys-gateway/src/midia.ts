import { lookup } from 'node:dns/promises';
import http from 'node:http';
import https from 'node:https';
import { BlockList, isIP } from 'node:net';

export class ErroDeMidia extends Error {
  constructor(
    message: string,
    readonly status: number = 400
  ) {
    super(message);
  }
}

export interface OpcoesDownload {
  limiteBytes: number;
  timeoutMs: number;
  hostsPermitidos: string[];
  requisitar?: Requisitar;
  resolverDns?: (host: string) => Promise<string[]>;
}

export interface RespostaHttp {
  status: number;
  contentType: string;
  contentLength: number;
  corpo: Buffer;
}

export type Requisitar = (url: URL, ip: string, timeoutMs: number, limiteBytes: number) => Promise<RespostaHttp>;

export const requisitarNoIpFixo: Requisitar = (url, ip, timeoutMs, limiteBytes) =>
  new Promise((resolver, rejeitar) => {
    const cliente = url.protocol === 'https:' ? https : http;
    const familia = isIP(ip);
    const pedido = cliente.request(
      url,
      {
        method: 'GET',
        timeout: timeoutMs,
        servername: isIP(url.hostname) ? undefined : url.hostname,
        lookup: (_host, opcoesLookup, retorno) => {
          const cb = retorno as unknown as (erro: Error | null, endereco: unknown, familia?: number) => void;
          if (typeof opcoesLookup === 'object' && opcoesLookup !== null && opcoesLookup.all) {
            cb(null, [{ address: ip, family: familia }]);
            return;
          }
          cb(null, ip, familia);
        }
      },
      (resposta) => {
        const pedacos: Buffer[] = [];
        let total = 0;
        resposta.on('data', (pedaco: Buffer) => {
          total += pedaco.length;
          if (total > limiteBytes) {
            pedido.destroy(new ErroDeMidia('imagem acima do limite'));
            return;
          }
          pedacos.push(pedaco);
        });
        resposta.on('end', () =>
          resolver({
            status: resposta.statusCode ?? 0,
            contentType: String(resposta.headers['content-type'] ?? ''),
            contentLength: Number(resposta.headers['content-length'] ?? 0),
            corpo: Buffer.concat(pedacos)
          })
        );
        resposta.on('error', rejeitar);
      }
    );
    pedido.on('timeout', () => pedido.destroy(new Error('tempo esgotado')));
    pedido.on('error', rejeitar);
    pedido.end();
  });

export interface Imagem {
  buffer: Buffer;
  mimetype: string;
}

const bloqueioIpv4 = new BlockList();
for (const [rede, prefixo] of [
  ['0.0.0.0', 8],
  ['10.0.0.0', 8],
  ['100.64.0.0', 10],
  ['127.0.0.0', 8],
  ['169.254.0.0', 16],
  ['172.16.0.0', 12],
  ['192.0.0.0', 24],
  ['192.0.2.0', 24],
  ['192.168.0.0', 16],
  ['198.18.0.0', 15],
  ['198.51.100.0', 24],
  ['203.0.113.0', 24],
  ['224.0.0.0', 3]
] as Array<[string, number]>) {
  bloqueioIpv4.addSubnet(rede, prefixo, 'ipv4');
}

const bloqueioIpv6 = new BlockList();
for (const [rede, prefixo] of [
  ['::', 96],
  ['::ffff:0:0:0', 96],
  ['64:ff9b:1::', 48],
  ['2001::', 32],
  ['100::', 64],
  ['fc00::', 7],
  ['fe80::', 10],
  ['fec0::', 10],
  ['ff00::', 8],
  ['2001:db8::', 32]
] as Array<[string, number]>) {
  bloqueioIpv6.addSubnet(rede, prefixo, 'ipv6');
}

function bytesIpv6(endereco: string): number[] | null {
  const semZona = endereco.split('%')[0];
  let texto = semZona;
  const ultimo = texto.lastIndexOf(':');
  const cauda = texto.slice(ultimo + 1);
  if (cauda.includes('.')) {
    if (isIP(cauda) !== 4) {
      return null;
    }
    const [a, b, c, d] = cauda.split('.').map(Number);
    texto = `${texto.slice(0, ultimo + 1)}${((a << 8) | b).toString(16)}:${((c << 8) | d).toString(16)}`;
  }
  const partes = texto.split('::');
  if (partes.length > 2) {
    return null;
  }
  const esquerda = partes[0] === '' ? [] : partes[0].split(':');
  const direita = partes.length === 2 && partes[1] !== '' ? partes[1].split(':') : [];
  const faltam = 8 - esquerda.length - direita.length;
  if ((partes.length === 1 && faltam !== 0) || faltam < 0) {
    return null;
  }
  const grupos = [...esquerda, ...Array<string>(partes.length === 2 ? faltam : 0).fill('0'), ...direita];
  if (grupos.length !== 8) {
    return null;
  }
  const bytes: number[] = [];
  for (const grupo of grupos) {
    if (!/^[0-9a-fA-F]{1,4}$/.test(grupo)) {
      return null;
    }
    const valor = Number.parseInt(grupo, 16);
    bytes.push(valor >> 8, valor & 255);
  }
  return bytes;
}

function ipv4Embutido(bytes: number[]): string | null {
  const zeros = (de: number, ate: number) => bytes.slice(de, ate).every((b) => b === 0);
  const quatro = (de: number) => bytes.slice(de, de + 4).join('.');
  if (zeros(0, 10) && bytes[10] === 255 && bytes[11] === 255) {
    return quatro(12);
  }
  if (bytes[0] === 0x00 && bytes[1] === 0x64 && bytes[2] === 0xff && bytes[3] === 0x9b && zeros(4, 12)) {
    return quatro(12);
  }
  if (zeros(0, 8) && bytes[8] === 255 && bytes[9] === 255 && bytes[10] === 0 && bytes[11] === 0) {
    return quatro(12);
  }
  if (bytes[0] === 0x00 && bytes[1] === 0x64 && bytes[2] === 0xff && bytes[3] === 0x9b && bytes[4] === 0x00 && bytes[5] === 0x01) {
    return quatro(12);
  }
  if (bytes[0] === 0x20 && bytes[1] === 0x02) {
    return quatro(2);
  }
  return null;
}

export function enderecoPrivado(endereco: string): boolean {
  const tipo = isIP(endereco.split('%')[0]);
  if (tipo === 4) {
    return bloqueioIpv4.check(endereco, 'ipv4');
  }
  if (tipo !== 6) {
    return true;
  }
  const bytes = bytesIpv6(endereco);
  if (bytes === null) {
    return true;
  }
  const embutido = ipv4Embutido(bytes);
  if (embutido !== null && bloqueioIpv4.check(embutido, 'ipv4')) {
    return true;
  }
  const normalizado = Array.from({ length: 8 }, (_, i) => ((bytes[2 * i] << 8) | bytes[2 * i + 1]).toString(16)).join(':');
  return bloqueioIpv6.check(normalizado, 'ipv6');
}

async function resolverPadrao(host: string): Promise<string[]> {
  const resultados = await lookup(host, { all: true });
  return resultados.map((r) => r.address);
}

export function lerDataUrl(valor: string, limiteBytes: number): Imagem {
  const casamento = /^data:([a-zA-Z0-9.+/-]+);base64,([A-Za-z0-9+/=\s]+)$/.exec(valor);
  if (!casamento) {
    throw new ErroDeMidia('data URL de imagem invalida');
  }
  const mimetype = casamento[1].toLowerCase();
  if (!mimetype.startsWith('image/')) {
    throw new ErroDeMidia('o arquivo nao e uma imagem');
  }
  const buffer = Buffer.from(casamento[2].replace(/\s/g, ''), 'base64');
  if (buffer.length === 0 || buffer.length > limiteBytes) {
    throw new ErroDeMidia('imagem vazia ou acima do limite');
  }
  return { buffer, mimetype };
}

export async function baixarImagem(origem: string, opcoes: OpcoesDownload): Promise<Imagem> {
  if (origem.startsWith('data:')) {
    return lerDataUrl(origem, opcoes.limiteBytes);
  }
  let url: URL;
  try {
    url = new URL(origem);
  } catch {
    throw new ErroDeMidia('url da imagem invalida');
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new ErroDeMidia('url da imagem deve ser http ou https');
  }
  if (url.username !== '' || url.password !== '') {
    throw new ErroDeMidia('url da imagem nao pode ter credenciais');
  }
  const host = url.hostname.toLowerCase().replace(/^\[|\]$/g, '');
  if (opcoes.hostsPermitidos.length > 0 && !opcoes.hostsPermitidos.includes(host)) {
    throw new ErroDeMidia('host da imagem nao permitido');
  }
  const enderecos = isIP(host) ? [host] : await (opcoes.resolverDns ?? resolverPadrao)(host).catch(() => []);
  if (enderecos.length === 0) {
    throw new ErroDeMidia('nao foi possivel resolver o host da imagem', 502);
  }
  if (enderecos.some(enderecoPrivado)) {
    throw new ErroDeMidia('host da imagem aponta para rede interna');
  }

  const ip = enderecos[0];
  try {
    const resposta = await (opcoes.requisitar ?? requisitarNoIpFixo)(url, ip, opcoes.timeoutMs, opcoes.limiteBytes);
    if (resposta.status < 200 || resposta.status >= 300) {
      throw new ErroDeMidia(`download da imagem falhou com http ${resposta.status}`, 502);
    }
    const tipo = resposta.contentType.split(';')[0].trim().toLowerCase();
    if (!tipo.startsWith('image/')) {
      throw new ErroDeMidia('o arquivo baixado nao e uma imagem');
    }
    if (resposta.contentLength > opcoes.limiteBytes || resposta.corpo.length > opcoes.limiteBytes) {
      throw new ErroDeMidia('imagem acima do limite');
    }
    if (resposta.corpo.length === 0) {
      throw new ErroDeMidia('imagem vazia');
    }
    return { buffer: resposta.corpo, mimetype: tipo };
  } catch (erro) {
    if (erro instanceof ErroDeMidia) {
      throw erro;
    }
    throw new ErroDeMidia('download da imagem falhou', 502);
  }
}
