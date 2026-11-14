import { mkdir, mkdtemp, writeFile } from 'node:fs/promises';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { vi } from 'vitest';
import { usarAutenticacaoAtomica } from '../src/auth-state.js';
import { carregarConfig, type Config } from '../src/config.js';
import { GerenciadorSessoes } from '../src/gerenciador.js';
import { criarLogger } from '../src/logger.js';
import { criarApp } from '../src/rotas.js';
import { gerarToken } from '../src/seguranca.js';
import type { DependenciasSessao, SocketLike } from '../src/sessao.js';

export const SEGREDO = 'segredo-de-teste-12345678901234567890';
export const SEGREDO_WEBHOOK = 'segredo-webhook-teste-123456789012';
export const DONO = '5511900000000:12@s.whatsapp.net';

export class SocketFalso implements SocketLike {
  ouvintes = new Map<string, Array<(...argumentos: any[]) => void>>();
  user: { id: string; lid?: string } | undefined = { id: DONO, lid: '88880000@lid' };
  enviados: Array<{ jid: string; conteudo: any; opcoes: any }> = [];
  presencas: Array<[string, string | undefined]> = [];
  existentes: Record<string, string> = {};
  logouts = 0;
  encerrado = 0;
  codigoPareamento = 'abcd1234';
  portaoEnvio: Promise<void> | null = null;
  falhaDeEnvio: Error | null = null;
  signalRepository: any = undefined;

  ev = {
    on: (evento: string, ouvinte: (...argumentos: any[]) => void) => {
      const lista = this.ouvintes.get(evento) ?? [];
      lista.push(ouvinte);
      this.ouvintes.set(evento, lista);
    }
  };

  emitir(evento: string, dados: unknown): void {
    (this.ouvintes.get(evento) ?? []).forEach((ouvinte) => ouvinte(dados));
  }

  async sendMessage(jid: string, conteudo: any, opcoes?: any): Promise<unknown> {
    if (this.portaoEnvio) {
      await this.portaoEnvio;
    }
    if (this.falhaDeEnvio) {
      const erro = this.falhaDeEnvio;
      this.falhaDeEnvio = null;
      throw erro;
    }
    this.enviados.push({ jid, conteudo, opcoes });
    return { key: { id: opcoes?.messageId } };
  }

  async sendPresenceUpdate(tipo: string, jid?: string): Promise<void> {
    this.presencas.push([tipo, jid]);
  }

  async onWhatsApp(...jids: string[]): Promise<Array<{ jid: string; exists: boolean }>> {
    return jids.map((jid) => {
      const resolvido = this.existentes[jid];
      return resolvido ? { jid: resolvido, exists: true } : { jid, exists: false };
    });
  }

  falhaDePareamento: Error | null = null;

  async requestPairingCode(): Promise<string> {
    if (this.falhaDePareamento) {
      throw this.falhaDePareamento;
    }
    return this.codigoPareamento;
  }

  async logout(): Promise<void> {
    this.logouts++;
  }

  end(): void {
    this.encerrado++;
  }
}

export interface Agendado {
  fn: () => void;
  ms: number;
  cancelado: boolean;
}

export interface Ambiente {
  config: Config;
  pasta: string;
  sockets: SocketFalso[];
  postagens: Array<{ url: string; sessao: string; payload: any }>;
  agendados: Agendado[];
  dormidas: number[];
  gerenciador: GerenciadorSessoes;
  porta: number;
  servidor: Server;
  token: (sessao: string) => string;
  chamar: (metodo: string, caminho: string, opcoes?: { corpo?: unknown; sessao?: string; token?: string | null; bruto?: string }) => Promise<Resposta>;
  fechar: () => Promise<void>;
  salvarCredenciais: (sessao: string, webhook?: string, creds?: Record<string, unknown>) => Promise<void>;
  conectar: (sessao: string, webhook?: string) => Promise<SocketFalso>;
}

export interface Resposta {
  status: number;
  tipo: string;
  json: any;
  bytes: Buffer;
}

export const CREDS_PAREADAS_POR_QR = {
  registered: false,
  me: { id: '5511999990000:12@s.whatsapp.net', lid: '99887766:12@lid', name: 'Dono' },
  account: { details: 'Cg4I', accountSignatureKey: 'abc', accountSignature: 'def', deviceSignature: 'ghi' }
};

export const CREDS_CODIGO_NAO_DIGITADO = {
  registered: false,
  me: { id: '5511999990000:12@s.whatsapp.net', name: '+55 11 99999-0000' }
};

export interface OpcoesAmbiente {
  env?: Record<string, string>;
  deps?: Partial<DependenciasSessao>;
  download?: Record<string, unknown>;
  logger?: ReturnType<typeof criarLogger>;
}

export async function montar(opcoes: OpcoesAmbiente = {}): Promise<Ambiente> {
  const pasta = await mkdtemp(join(tmpdir(), 'baileys-gw-'));
  const config = carregarConfig({
    SECRET_KEY: SEGREDO,
    WPPCONNECT_WEBHOOK_SECRET: SEGREDO_WEBHOOK,
    BAILEYS_AUTH_DIR: pasta,
    BAILEYS_HOSTS_WEBHOOK_PERMITIDOS: 'bot-java,destino,java,java-do-meta,reserva',
    ...opcoes.env
  });
  const sockets: SocketFalso[] = [];
  const postagens: Ambiente['postagens'] = [];
  const agendados: Agendado[] = [];
  const dormidas: number[] = [];
  const log = opcoes.logger ?? criarLogger('silent');
  const despachante = {
    postar: vi.fn(async (url: string, sessao: string, payload: unknown) => {
      postagens.push({ url, sessao, payload });
      return { entregue: true as const, tentativas: 1 };
    })
  };
  const gerenciador = new GerenciadorSessoes({
    config,
    log,
    fabrica: async () => {
      const socket = new SocketFalso();
      sockets.push(socket);
      return socket;
    },
    carregarAutenticacao: usarAutenticacaoAtomica,
    despachante,
    agendar: (fn, ms) => {
      const item: Agendado = { fn, ms, cancelado: false };
      agendados.push(item);
      return item;
    },
    cancelar: (item) => {
      (item as Agendado).cancelado = true;
    },
    dormir: async (ms) => {
      dormidas.push(ms);
    },
    aleatorio: () => 0.5,
    ...opcoes.deps
  });
  const app = criarApp({ config, log, gerenciador, opcoesDownload: opcoes.download as never });
  const servidor = await new Promise<Server>((resolver) => {
    const instancia = app.listen(0, '127.0.0.1', () => resolver(instancia));
  });
  const porta = (servidor.address() as AddressInfo).port;
  const token = (sessao: string): string => gerarToken(SEGREDO, sessao);

  const chamar: Ambiente['chamar'] = async (metodo, caminho, opcoesChamada = {}) => {
    const cabecalhos: Record<string, string> = { 'Content-Type': 'application/json' };
    if (opcoesChamada.token !== null) {
      const sessao = opcoesChamada.sessao ?? caminho.split('/')[2];
      cabecalhos.Authorization = `Bearer ${opcoesChamada.token ?? token(sessao)}`;
    }
    const resposta = await fetch(`http://127.0.0.1:${porta}${caminho}`, {
      method: metodo,
      headers: cabecalhos,
      body: metodo === 'GET' ? undefined : (opcoesChamada.bruto ?? (opcoesChamada.corpo === undefined ? undefined : JSON.stringify(opcoesChamada.corpo)))
    });
    const bytes = Buffer.from(await resposta.arrayBuffer());
    const tipo = resposta.headers.get('content-type') ?? '';
    let json: any = null;
    if (tipo.includes('json')) {
      json = JSON.parse(bytes.toString('utf8'));
    }
    return { status: resposta.status, tipo, json, bytes };
  };

  const salvarCredenciais = async (sessao: string, webhook?: string, creds: Record<string, unknown> = CREDS_PAREADAS_POR_QR): Promise<void> => {
    const dir = join(pasta, sessao);
    await mkdir(dir, { recursive: true });
    await writeFile(join(dir, 'creds.json'), JSON.stringify(creds));
    if (webhook) {
      await writeFile(join(dir, 'meta.json'), JSON.stringify({ webhook }));
    }
  };

  const conectar = async (sessao: string, webhook?: string): Promise<SocketFalso> => {
    await chamar('POST', `/api/${sessao}/start-session`, { corpo: { webhook: webhook ?? 'http://bot-java:8080/webhook/whatsapp', waitQrCode: false } });
    const socket = sockets[sockets.length - 1];
    socket.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(() => {
      if (gerenciador.obter(sessao)?.estado !== 'CONNECTED') {
        throw new Error('ainda nao conectou');
      }
    });
    return socket;
  };

  return {
    config,
    pasta,
    sockets,
    postagens,
    agendados,
    dormidas,
    gerenciador,
    porta,
    servidor,
    token,
    chamar,
    fechar: async () => {
      await gerenciador.parar();
      await new Promise<void>((resolver) => servidor.close(() => resolver()));
    },
    salvarCredenciais,
    conectar
  };
}

export function mensagemTexto(sobrescrever: Record<string, any> = {}): any {
  return {
    key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: false, id: 'ABC123' },
    message: { conversation: 'Olá, gostaria de reservar' },
    messageTimestamp: 1730592000,
    pushName: 'Joao Silva',
    ...sobrescrever
  };
}
