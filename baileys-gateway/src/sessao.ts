import { randomBytes } from 'node:crypto';
import { rm } from 'node:fs/promises';
import { join } from 'node:path';
import type { Logger } from 'pino';
import type { EstadoDeAutenticacao } from './auth-state.js';
import { gravarAtomico } from './arquivos.js';
import type { Config } from './config.js';
import { FilaDeEnvio } from './fila.js';
import { normalizar, usuarioDe } from './jid.js';
import { mascararNumero, tamanhoTexto } from './mascara.js';
import { converterMensagem, type MensagemBaileys } from './mensagem.js';
import { gerarQrPng } from './qr.js';
import { IdsRecentes, MapaRecente } from './recentes.js';
import type { DespachanteWebhook } from './webhook.js';

export type EstadoSessao = 'CONNECTED' | 'QRCODE' | 'INITIALIZING' | 'DISCONNECTED';

export const CODIGOS = {
  connectionClosed: 428,
  connectionLost: 408,
  connectionReplaced: 440,
  timedOut: 408,
  loggedOut: 401,
  badSession: 500,
  restartRequired: 515,
  multideviceMismatch: 411,
  unavailableService: 503
} as const;

export interface SocketLike {
  ev: { on: (evento: string, ouvinte: (...argumentos: any[]) => void) => void };
  user?: { id: string; lid?: string } | undefined;
  sendMessage: (jid: string, conteudo: any, opcoes?: any) => Promise<unknown>;
  sendPresenceUpdate: (tipo: string, jid?: string) => Promise<void>;
  onWhatsApp: (...jids: string[]) => Promise<Array<{ jid: string; exists: boolean }> | undefined>;
  requestPairingCode: (telefone: string) => Promise<string>;
  logout: (mensagem?: string) => Promise<void>;
  end: (erro?: Error) => void;
  signalRepository?: any;
}

export type FabricaSocket = (opcoes: { sessao: string; auth: EstadoDeAutenticacao }) => Promise<SocketLike>;

export interface DependenciasSessao {
  config: Config;
  log: Logger;
  fabrica: FabricaSocket;
  carregarAutenticacao: (pasta: string) => Promise<EstadoDeAutenticacao>;
  despachante: Pick<DespachanteWebhook, 'postar'>;
  agendar?: (acao: () => void, ms: number) => unknown;
  cancelar?: (id: unknown) => void;
  dormir?: (ms: number) => Promise<void>;
  agora?: () => number;
  aleatorio?: () => number;
  opcoesFila?: ConstructorParameters<typeof FilaDeEnvio>[0];
  esperaReenvioMs?: number;
}

export class ErroDeNegocio extends Error {
  constructor(
    readonly status: number,
    message: string
  ) {
    super(message);
  }
}

class SocketIndisponivel extends Error {}

const ESPERA_MAXIMA_MS = 60000;
const ESPERA_INICIAL_MS = 2000;
const JANELA_PAREAMENTO_MS = 10 * 60 * 1000;
const LIMITE_PAREAMENTO = 3;
const PARES_LID_MAXIMO = 5000;
const PARES_LID_VALIDADE_MS = 24 * 60 * 60 * 1000;

function codigoDe(erro: unknown): number | undefined {
  const candidato = erro as { output?: { statusCode?: number }; statusCode?: number } | undefined;
  return candidato?.output?.statusCode ?? candidato?.statusCode;
}

function falhaDeConexao(erro: unknown): boolean {
  if (erro instanceof SocketIndisponivel) {
    return true;
  }
  const codigo = codigoDe(erro);
  if (codigo === CODIGOS.connectionClosed || codigo === CODIGOS.connectionLost || codigo === CODIGOS.unavailableService) {
    return true;
  }
  return erro instanceof Error && /connection closed|socket|not connected/i.test(erro.message);
}

export class Sessao {
  estado: EstadoSessao = 'DISCONNECTED';
  webhook: string | null = null;
  readonly fila: FilaDeEnvio;
  readonly pasta: string;

  private qr: string | null = null;
  private sock: SocketLike | null = null;
  private auth: EstadoDeAutenticacao | null = null;
  private geracao = 0;
  private tentativas = 0;
  private reinicios = 0;
  private aberturas = 0;
  private timer: unknown = null;
  private encerrada = false;
  private abrindo: Promise<void> | null = null;
  private numeroConectado: string | null = null;
  private lidConectado: string | null = null;
  private primeiroQrVisto = false;
  private pedidosPareamento: number[] = [];
  private esperandoConexao: Array<() => void> = [];
  private esperandoQr: Array<() => void> = [];
  private readonly paresLid: MapaRecente<string>;
  private readonly enviados = new IdsRecentes(24 * 60 * 60 * 1000, 5000);
  private readonly postados = new IdsRecentes(10 * 60 * 1000, 5000);
  private readonly agendar: (acao: () => void, ms: number) => unknown;
  private readonly cancelar: (id: unknown) => void;
  private readonly dormir: (ms: number) => Promise<void>;
  private readonly agora: () => number;

  constructor(
    readonly nome: string,
    private readonly deps: DependenciasSessao
  ) {
    this.pasta = join(deps.config.pastaAutenticacao, nome);
    this.agendar = deps.agendar ?? ((acao, ms) => setTimeout(acao, ms));
    this.cancelar = deps.cancelar ?? ((id) => clearTimeout(id as NodeJS.Timeout));
    this.dormir = deps.dormir ?? ((ms) => new Promise((resolver) => setTimeout(resolver, ms)));
    this.agora = deps.agora ?? (() => Date.now());
    this.paresLid = new MapaRecente<string>(PARES_LID_VALIDADE_MS, PARES_LID_MAXIMO, this.agora);
    this.fila = new FilaDeEnvio({ dormir: this.dormir, aleatorio: deps.aleatorio, ...deps.opcoesFila });
  }

  get conectada(): boolean {
    return this.estado === 'CONNECTED' && this.sock !== null;
  }

  status(): { status: EstadoSessao; qrcode: string | null } {
    return { status: this.estado, qrcode: this.estado === 'QRCODE' ? this.qr : null };
  }

  qrAtual(): string | null {
    return this.estado === 'QRCODE' ? this.qr : null;
  }

  definirWebhook(url: string | null): void {
    this.webhook = url;
  }

  async gravarMeta(): Promise<void> {
    if (!this.webhook) {
      return;
    }
    await gravarAtomico(join(this.pasta, 'meta.json'), JSON.stringify({ webhook: this.webhook }));
  }

  async iniciar(): Promise<void> {
    if (this.sock || this.timer !== null) {
      return;
    }
    if (this.abrindo) {
      return this.abrindo;
    }
    this.abrindo = this.abrir().finally(() => {
      this.abrindo = null;
    });
    return this.abrindo;
  }

  private async abrir(): Promise<void> {
    this.encerrada = false;
    try {
      this.auth = await this.deps.carregarAutenticacao(this.pasta);
      const registrada = this.auth.registrada();
      this.estado = registrada ? 'INITIALIZING' : 'QRCODE';
      this.qr = null;
      this.primeiroQrVisto = false;
      const geracao = ++this.geracao;
      const sock = await this.deps.fabrica({ sessao: this.nome, auth: this.auth });
      if (geracao !== this.geracao || this.encerrada) {
        sock.end(undefined);
        return;
      }
      this.sock = sock;
      sock.ev.on('creds.update', () => {
        if (geracao !== this.geracao) {
          return;
        }
        void this.auth?.salvarCredenciais().catch(() => {
          this.deps.log.error({ sessao: this.nome }, 'Falha ao gravar credenciais');
        });
      });
      sock.ev.on('connection.update', (atualizacao: any) => {
        void this.aoAtualizarConexao(geracao, atualizacao);
      });
      sock.ev.on('messages.upsert', (evento: any) => {
        void this.aoReceber(geracao, evento);
      });
      sock.ev.on('contacts.upsert', (contatos: any[]) => this.aprenderContatos(contatos));
      sock.ev.on('contacts.update', (contatos: any[]) => this.aprenderContatos(contatos));
    } catch (erro) {
      this.deps.log.error({ sessao: this.nome, erro }, 'Falha ao abrir o socket');
      this.sock = null;
      this.reagendarPorRede();
    }
  }

  private aprenderContatos(contatos: any[]): void {
    for (const contato of contatos ?? []) {
      const lid = usuarioDe(contato?.lid);
      const numero = contato?.id && !String(contato.id).includes('@lid') ? normalizar(contato.id) : null;
      if (lid && numero) {
        this.paresLid.set(`${lid}@lid`, numero);
      }
    }
  }

  private async aoAtualizarConexao(
    geracao: number,
    atualizacao: { connection?: string; lastDisconnect?: { error?: unknown }; qr?: string }
  ): Promise<void> {
    if (geracao !== this.geracao) {
      return;
    }
    if (atualizacao.qr) {
      await this.aoNovoQr(geracao, atualizacao.qr);
    }
    if (atualizacao.connection === 'open') {
      this.aoAbrirConexao();
    } else if (atualizacao.connection === 'close') {
      await this.aoFecharConexao(geracao, atualizacao.lastDisconnect?.error);
    }
  }

  private async aoNovoQr(geracao: number, texto: string): Promise<void> {
    const png = await gerarQrPng(texto);
    if (geracao !== this.geracao) {
      return;
    }
    this.qr = png;
    this.estado = 'QRCODE';
    this.tentativas = 0;
    this.primeiroQrVisto = true;
    this.esperandoQr.splice(0).forEach((liberar) => liberar());
    this.deps.log.info({ sessao: this.nome }, 'QR Code renovado');
    this.publicar({ event: 'qrcode', session: this.nome, qrcode: png });
  }

  private aoAbrirConexao(): void {
    this.estado = 'CONNECTED';
    this.aberturas++;
    this.qr = null;
    this.tentativas = 0;
    this.reinicios = 0;
    this.numeroConectado = normalizar(this.sock?.user?.id);
    this.lidConectado = this.sock?.user?.lid ?? null;
    this.deps.log.info({ sessao: this.nome, numero: mascararNumero(this.numeroConectado) }, 'Sessao conectada');
    this.esperandoConexao.splice(0).forEach((liberar) => liberar());
    this.publicar({ event: 'status-find', session: this.nome, status: 'inChat' });
  }

  private async aoFecharConexao(geracao: number, erro: unknown): Promise<void> {
    this.sock = null;
    if (this.encerrada) {
      return;
    }
    const codigo = codigoDe(erro);
    this.deps.log.warn({ sessao: this.nome, codigo }, 'Conexao encerrada');
    if (codigo === CODIGOS.connectionReplaced) {
      this.estado = 'DISCONNECTED';
      this.qr = null;
      this.deps.log.error({ sessao: this.nome }, 'Conflito: a mesma sessao foi aberta em outra instancia; sem nova tentativa');
      return;
    }
    if (codigo === CODIGOS.loggedOut || codigo === CODIGOS.badSession || codigo === CODIGOS.multideviceMismatch) {
      await this.apagarCredenciais();
      if (geracao !== this.geracao) {
        return;
      }
      this.estado = 'DISCONNECTED';
      this.qr = null;
      this.deps.log.warn({ sessao: this.nome, codigo }, 'Credenciais apagadas; novo pareamento necessario');
      this.publicar({ event: 'status-find', session: this.nome, status: 'desconnectedMobile' });
      return;
    }
    if (codigo === CODIGOS.restartRequired && this.reinicios < 3) {
      this.reinicios++;
      this.estado = this.auth?.registrada() ? 'INITIALIZING' : 'QRCODE';
      this.agendarAbertura(0);
      return;
    }
    this.reagendarPorRede();
  }

  private reagendarPorRede(): void {
    const espera = Math.min(ESPERA_MAXIMA_MS, ESPERA_INICIAL_MS * 2 ** this.tentativas);
    this.tentativas = Math.min(this.tentativas + 1, 10);
    this.estado = this.auth?.registrada() ? 'INITIALIZING' : 'QRCODE';
    this.qr = null;
    this.agendarAbertura(espera);
  }

  private agendarAbertura(ms: number): void {
    if (this.timer !== null) {
      this.cancelar(this.timer);
    }
    this.timer = this.agendar(() => {
      this.timer = null;
      void this.iniciar();
    }, ms);
  }

  private async apagarCredenciais(): Promise<void> {
    this.auth = null;
    await rm(this.pasta, { recursive: true, force: true });
  }

  private publicar(payload: unknown): void {
    const url = this.webhook ?? this.deps.config.urlWebhookPadrao;
    void this.deps.despachante.postar(url, this.nome, payload);
  }

  private async resolverLid(chat: string | null | undefined): Promise<void> {
    const usuario = usuarioDe(chat);
    if (!chat || !chat.includes('@lid') || !usuario || this.paresLid.has(`${usuario}@lid`)) {
      return;
    }
    try {
      const repositorio = this.sock?.signalRepository?.lidMapping;
      const numero = await repositorio?.getPNForLID?.(`${usuario}@lid`);
      if (numero) {
        this.paresLid.set(`${usuario}@lid`, normalizar(String(numero)) as string);
      }
    } catch {
      return;
    }
  }

  private async aoReceber(geracao: number, evento: { messages?: MensagemBaileys[]; type?: string }): Promise<void> {
    if (geracao !== this.geracao || evento?.type !== 'notify') {
      return;
    }
    for (const mensagem of evento.messages ?? []) {
      try {
        await this.tratarMensagem(mensagem);
      } catch (erro) {
        this.deps.log.error({ sessao: this.nome, erro }, 'Falha ao tratar mensagem recebida');
      }
    }
  }

  private async tratarMensagem(mensagem: MensagemBaileys): Promise<void> {
    await this.resolverLid(mensagem.key?.remoteJid);
    const chave = mensagem.key;
    const lid = chave?.remoteJid && chave.remoteJid.includes('@lid') ? chave.remoteJid : null;
    const parNoEvento = chave?.remoteJidAlt ?? (chave?.fromMe ? null : chave?.senderPn) ?? null;
    if (lid && parNoEvento && !parNoEvento.includes('@lid')) {
      this.paresLid.set(`${usuarioDe(lid)}@lid`, normalizar(parNoEvento) as string);
    }
    const resultado = converterMensagem(mensagem, {
      sessao: this.nome,
      numeroConectado: this.numeroConectado,
      lidConectado: this.lidConectado,
      aoMensagemDoDono: this.deps.config.aoMensagemDoDono,
      ehEcoDoBot: (id) => this.enviados.contem(id),
      resolverNumeroDoLid: (valor) => this.paresLid.get(valor)
    });
    if (!resultado.ok) {
      this.deps.log.debug({ sessao: this.nome, motivo: resultado.motivo }, 'Mensagem ignorada');
      return;
    }
    if (!this.postados.registrarSeNovo(resultado.payload.id)) {
      this.deps.log.debug({ sessao: this.nome }, 'Mensagem repetida ignorada');
      return;
    }
    const alvo = resultado.payload.event === 'onselfmessage' ? resultado.payload.to : resultado.payload.from;
    this.deps.log.info(
      {
        sessao: this.nome,
        evento: resultado.payload.event,
        numero: mascararNumero(alvo),
        tipo: resultado.payload.type,
        texto: tamanhoTexto(resultado.payload.body)
      },
      'Mensagem recebida'
    );
    this.publicar(resultado.payload);
  }

  async resolverDestinoDeNumero(digitos: string): Promise<string | null> {
    const sock = this.sock;
    if (!sock || this.estado !== 'CONNECTED') {
      throw new SocketIndisponivel();
    }
    const respostas = await sock.onWhatsApp(`${digitos}@s.whatsapp.net`);
    const achado = respostas?.find((resposta) => resposta.exists);
    return achado ? achado.jid : null;
  }

  enfileirarTexto(jid: string, texto: string): boolean {
    const duracao = Math.min(3000, Math.max(800, texto.length * 40));
    return this.enfileirar(jid, { text: texto }, duracao);
  }

  enfileirarImagem(jid: string, imagem: Buffer, mimetype: string, legenda: string | undefined): boolean {
    const conteudo: Record<string, unknown> = { image: imagem, mimetype };
    if (legenda) {
      conteudo.caption = legenda;
    }
    return this.enfileirar(jid, conteudo, 1000);
  }

  private enfileirar(jid: string, conteudo: Record<string, unknown>, digitacaoMs: number): boolean {
    const id = `3EB0${randomBytes(16).toString('hex').toUpperCase()}`;
    this.enviados.adicionar(id);
    return this.fila.enfileirar(() => this.executarEnvio(jid, conteudo, digitacaoMs, id));
  }

  private async tentarEnvio(jid: string, conteudo: Record<string, unknown>, digitacaoMs: number, id: string): Promise<void> {
    const sock = this.sock;
    if (!sock || this.estado !== 'CONNECTED') {
      throw new SocketIndisponivel();
    }
    if (this.deps.config.simularDigitacao) {
      try {
        await sock.sendPresenceUpdate('composing', jid);
        await this.dormir(digitacaoMs);
        await sock.sendPresenceUpdate('paused', jid);
      } catch (erro) {
        if (falhaDeConexao(erro)) {
          throw erro;
        }
      }
    }
    await sock.sendMessage(jid, conteudo, { messageId: id });
  }

  private async executarEnvio(jid: string, conteudo: Record<string, unknown>, digitacaoMs: number, id: string): Promise<void> {
    const aberturasAntes = this.aberturas;
    try {
      await this.tentarEnvio(jid, conteudo, digitacaoMs, id);
      return;
    } catch (erro) {
      if (!falhaDeConexao(erro) && this.estado === 'CONNECTED') {
        this.deps.log.warn({ sessao: this.nome, numero: mascararNumero(jid), erro }, 'Envio descartado');
        return;
      }
    }
    this.deps.log.warn({ sessao: this.nome, numero: mascararNumero(jid) }, 'Envio adiado: socket indisponivel');
    const voltou = await this.aguardarConexao(this.deps.esperaReenvioMs ?? ESPERA_MAXIMA_MS, aberturasAntes);
    if (!voltou) {
      this.deps.log.warn({ sessao: this.nome, numero: mascararNumero(jid) }, 'Envio descartado: sessao nao reconectou a tempo');
      return;
    }
    try {
      await this.tentarEnvio(jid, conteudo, digitacaoMs, id);
    } catch (erro) {
      this.deps.log.warn({ sessao: this.nome, numero: mascararNumero(jid), erro }, 'Envio descartado apos nova tentativa');
    }
  }

  private aguardarConexao(ms: number, aberturasAntes: number): Promise<boolean> {
    if (this.conectada && this.aberturas > aberturasAntes) {
      return Promise.resolve(true);
    }
    return new Promise((resolver) => {
      let liberada = false;
      const limite = this.agendar(() => {
        if (!liberada) {
          liberada = true;
          this.esperandoConexao = this.esperandoConexao.filter((item) => item !== liberar);
          resolver(false);
        }
      }, ms);
      const liberar = (): void => {
        if (!liberada) {
          liberada = true;
          this.cancelar(limite);
          resolver(true);
        }
      };
      this.esperandoConexao.push(liberar);
    });
  }

  private aguardarPrimeiroQr(ms: number): Promise<void> {
    if (this.primeiroQrVisto) {
      return Promise.resolve();
    }
    return new Promise((resolver) => {
      const liberar = () => {
        this.cancelar(limite);
        resolver();
      };
      const limite = this.agendar(() => {
        const posicao = this.esperandoQr.indexOf(liberar);
        if (posicao >= 0) {
          this.esperandoQr.splice(posicao, 1);
        }
        resolver();
      }, ms);
      this.esperandoQr.push(liberar);
    });
  }

  async solicitarCodigoPareamento(telefone: string): Promise<string> {
    if (this.estado === 'CONNECTED') {
      throw new ErroDeNegocio(409, 'Sessao ja conectada');
    }
    if (!this.sock || this.estado !== 'QRCODE') {
      throw new ErroDeNegocio(409, 'Sessao nao esta aguardando pareamento');
    }
    const agora = this.agora();
    this.pedidosPareamento = this.pedidosPareamento.filter((momento) => agora - momento < JANELA_PAREAMENTO_MS);
    if (this.pedidosPareamento.length >= LIMITE_PAREAMENTO) {
      throw new ErroDeNegocio(429, 'Limite de pedidos de codigo atingido');
    }
    this.pedidosPareamento.push(agora);
    const desfazer = () => {
      const posicao = this.pedidosPareamento.lastIndexOf(agora);
      if (posicao >= 0) {
        this.pedidosPareamento.splice(posicao, 1);
      }
    };
    try {
      await this.aguardarPrimeiroQr(3000);
      const sock = this.sock as SocketLike | null;
      if (!sock || !this.primeiroQrVisto || this.estado !== 'QRCODE') {
        throw new ErroDeNegocio(409, 'Sessao ainda iniciando; tente de novo');
      }
      const bruto = await sock.requestPairingCode(telefone);
      const limpo = bruto.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
      return limpo.length === 8 ? `${limpo.slice(0, 4)}-${limpo.slice(4)}` : limpo;
    } catch (erro) {
      if (erro instanceof ErroDeNegocio && erro.status === 409) {
        desfazer();
      }
      if (erro instanceof ErroDeNegocio) {
        throw erro;
      }
      this.deps.log.warn({ sessao: this.nome, erro }, 'Falha ao gerar codigo de pareamento');
      throw new ErroDeNegocio(502, 'Falha ao gerar o codigo de pareamento');
    }
  }

  async fechar(): Promise<void> {
    this.encerrada = true;
    this.geracao++;
    if (this.timer !== null) {
      this.cancelar(this.timer);
      this.timer = null;
    }
    this.fila.descartarTudo();
    const sock = this.sock;
    const conectada = this.estado === 'CONNECTED';
    this.sock = null;
    if (sock) {
      if (conectada) {
        await Promise.race([
          sock.logout().catch(() => undefined),
          new Promise<void>((resolver) => setTimeout(resolver, 5000).unref())
        ]);
      }
      try {
        sock.end(undefined);
      } catch {
        this.deps.log.debug({ sessao: this.nome }, 'Socket ja encerrado');
      }
    }
    await this.apagarCredenciais();
    this.estado = 'DISCONNECTED';
    this.qr = null;
    this.esperandoConexao.splice(0).forEach((liberar) => liberar());
    this.esperandoQr.splice(0).forEach((liberar) => liberar());
  }

  async parar(): Promise<void> {
    this.encerrada = true;
    this.geracao++;
    if (this.timer !== null) {
      this.cancelar(this.timer);
      this.timer = null;
    }
    this.fila.descartarTudo();
    const sock = this.sock;
    this.sock = null;
    if (sock) {
      try {
        sock.end(undefined);
      } catch {
        this.deps.log.debug({ sessao: this.nome }, 'Socket ja encerrado');
      }
    }
    this.esperandoConexao.splice(0).forEach((liberar) => liberar());
    this.esperandoQr.splice(0).forEach((liberar) => liberar());
  }
}
