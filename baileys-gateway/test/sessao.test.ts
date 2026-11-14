import { access, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { criarLogger } from '../src/logger.js';
import { CREDS_CODIGO_NAO_DIGITADO, montar, mensagemTexto, SEGREDO, SEGREDO_WEBHOOK, type Ambiente, type SocketFalso } from './helpers.js';

let ambiente: Ambiente | undefined;

afterEach(async () => {
  await ambiente?.fechar();
  ambiente = undefined;
});

async function existe(caminho: string): Promise<boolean> {
  try {
    await access(caminho);
    return true;
  } catch {
    return false;
  }
}

const queda = (codigo: number) => ({ connection: 'close', lastDisconnect: { error: { output: { statusCode: codigo } } } });

async function estadoDe(nome: string): Promise<string | undefined> {
  return ambiente!.gerenciador.obter(nome)?.estado;
}

async function restaurar(env?: Record<string, string>): Promise<{ socket: SocketFalso }> {
  ambiente = await montar(env ? { env } : {});
  await ambiente.salvarCredenciais('sessao_a', 'http://java-do-meta/webhook');
  const total = await ambiente.gerenciador.restaurar();
  expect(total).toBe(1);
  return { socket: ambiente.sockets[0] };
}

describe('mensagens recebidas viram webhook', () => {
  async function conectada() {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a', 'http://java/webhook');
    ambiente.postagens.length = 0;
    return socket;
  }

  const upsert = (socket: SocketFalso, messages: unknown[], type = 'notify') => socket.emitir('messages.upsert', { messages, type });

  it('so o tipo notify e postado: historico (append) e sincronizacao nao viram webhook', async () => {
    const socket = await conectada();
    upsert(socket, [mensagemTexto()], 'append');
    upsert(socket, [mensagemTexto()], 'prepend');
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: false, id: 'N1' } })], 'notify');
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    expect(ambiente!.postagens[0].payload.id).toContain('N1');
    expect(ambiente!.postagens[0].url).toBe('http://java/webhook');
  });

  it('mensagem repetida na reconexao nao e reenviada ao webhook', async () => {
    const socket = await conectada();
    upsert(socket, [mensagemTexto()]);
    upsert(socket, [mensagemTexto()]);
    upsert(socket, [mensagemTexto()]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    await new Promise((resolver) => setTimeout(resolver, 30));
    expect(ambiente!.postagens).toHaveLength(1);
  });

  it('dono: onselfmessage com to do cliente, e o nome do dono fora do payload', async () => {
    const socket = await conectada();
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: true, id: 'D1' }, pushName: 'Dona Maria' })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    const payload = ambiente!.postagens[0].payload;
    expect(payload).toMatchObject({ event: 'onselfmessage', fromMe: true, from: '5511900000000@c.us', to: '5511988887777@c.us' });
    expect(JSON.stringify(payload)).not.toContain('Dona Maria');
  });

  it('WPPCONNECT_ON_SELF_MESSAGE=false nao despacha mensagem do dono', async () => {
    ambiente = await montar({ env: { WPPCONNECT_ON_SELF_MESSAGE: 'false' } });
    const socket = await ambiente.conectar('sessao_a');
    ambiente.postagens.length = 0;
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: true, id: 'D1' } })]);
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: false, id: 'C1' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    expect(ambiente!.postagens[0].payload.event).toBe('onmessage');
  });

  it('eco do envio do bot e descartado pelo id, mesmo muito depois do envio', async () => {
    const socket = await conectada();
    socket.existentes['5511988887777@s.whatsapp.net'] = '5511988887777@s.whatsapp.net';
    await ambiente!.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511988887777', message: 'Olá! Como posso ajudar?' } });
    await ambiente!.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    const idEnviado = socket.enviados[0].opcoes.messageId as string;

    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: true, id: idEnviado }, message: { conversation: 'Olá! Como posso ajudar?' } })]);
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: true, id: 'OUTRO' }, message: { conversation: 'digitei no celular' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    expect(ambiente!.postagens[0].payload.id).toContain('OUTRO');
  });

  it('ignora senderPn (e o numero do dono), nao aprende par e o cliente depois sai com o @lid', async () => {
    const socket = await conectada();
    upsert(socket, [mensagemTexto({ key: { remoteJid: '123456789012345@lid', fromMe: true, id: 'D1', senderPn: '5511999990000@s.whatsapp.net' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    const dono = ambiente!.postagens[0].payload;
    expect(dono.event).toBe('onselfmessage');
    expect(dono.to).toBe('123456789012345@lid');
    const paresLid = (ambiente!.gerenciador.obter('sessao_a') as unknown as { paresLid: { has(chave: string): boolean; get(chave: string): string | undefined } }).paresLid;
    expect(paresLid.has('123456789012345@lid')).toBe(false);
    expect(paresLid.get('123456789012345@lid')).toBeUndefined();
    upsert(socket, [mensagemTexto({ key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'C1' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(2));
    const cliente = ambiente!.postagens[1].payload;
    expect(cliente.from).toBe('123456789012345@lid');
    expect(cliente.sender.id).toBe('123456789012345@lid');
  });

  it('chat @lid com o numero no evento e no mapa de contatos', async () => {
    const socket = await conectada();
    upsert(socket, [mensagemTexto({ key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L1', senderPn: '5511999990000@s.whatsapp.net' } })]);
    socket.emitir('contacts.upsert', [{ id: '5511977776666@s.whatsapp.net', lid: '999888777666555@lid' }]);
    upsert(socket, [mensagemTexto({ key: { remoteJid: '999888777666555@lid', fromMe: false, id: 'L2' } })]);
    upsert(socket, [mensagemTexto({ key: { remoteJid: '555444333222111@lid', fromMe: false, id: 'L3' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(3));
    const porId = (parte: string) => ambiente!.postagens.map((p) => p.payload).find((payload) => payload.id.endsWith(parte));
    const [a, b, c] = [porId('_L1'), porId('_L2'), porId('_L3')];
    expect(a.from).toBe('123456789012345@lid');
    expect(a.sender.id).toBe('5511999990000@c.us');
    expect(b.from).toBe('999888777666555@lid');
    expect(b.sender.id).toBe('5511977776666@c.us');
    expect(c.sender.id).toBe('555444333222111@lid');
  });

  it('@lid consulta o mapa do proprio Baileys quando existe', async () => {
    const socket = await conectada();
    socket.signalRepository = { lidMapping: { getPNForLID: vi.fn(async () => '5511966665555@s.whatsapp.net') } };
    upsert(socket, [mensagemTexto({ key: { remoteJid: '111222333444555@lid', fromMe: false, id: 'M1' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    expect(ambiente!.postagens[0].payload.sender.id).toBe('5511966665555@c.us');
  });

  it('grupo, status e canal nao geram webhook', async () => {
    const socket = await conectada();
    for (const [indice, chat] of ['120363@g.us', 'status@broadcast', '1203@newsletter', '5511@broadcast'].entries()) {
      upsert(socket, [mensagemTexto({ key: { remoteJid: chat, fromMe: false, id: `G${indice}` } })]);
      upsert(socket, [mensagemTexto({ key: { remoteJid: chat, fromMe: true, id: `H${indice}` } })]);
    }
    upsert(socket, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: false, id: 'FIM' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(1));
    expect(ambiente!.postagens[0].payload.id).toContain('FIM');
  });

  it('o evento da sessao A usa o webhook gravado para ela, com reserva no padrao', async () => {
    ambiente = await montar({ env: { WPPCONNECT_WEBHOOK_URL: 'http://reserva:8080/webhook/whatsapp' } });
    const socket = await ambiente.conectar('sessao_a');
    await ambiente.chamar('POST', '/api/sessao_b/start-session', { corpo: {} });
    const socketB = ambiente.sockets[1];
    socketB.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_b')?.estado).toBe('CONNECTED'));
    ambiente.postagens.length = 0;
    upsert(socket, [mensagemTexto()]);
    upsert(socketB, [mensagemTexto({ key: { remoteJid: '5511988887777@s.whatsapp.net', fromMe: false, id: 'B1' } })]);
    await vi.waitFor(() => expect(ambiente!.postagens).toHaveLength(2));
    const porSessao = Object.fromEntries(ambiente.postagens.map((p) => [p.sessao, p.url]));
    expect(porSessao.sessao_a).toBe('http://bot-java:8080/webhook/whatsapp');
    expect(porSessao.sessao_b).toBe('http://reserva:8080/webhook/whatsapp');
  });
});

describe('webhook restrito ao host configurado', () => {
  it('start-session recusa host fora da lista com 400 e nao grava meta nem abre socket', async () => {
    ambiente = await montar({ env: { BAILEYS_HOSTS_WEBHOOK_PERMITIDOS: '' } });
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://atacante.exemplo:9/x' } });
    expect(resposta.status).toBe(400);
    expect(ambiente.sockets).toHaveLength(0);
    const interno = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://169.254.169.254/latest' } });
    expect(interno.status).toBe(400);
  });

  it('o padrao do compose (bot-java) continua aceito e a lista opcional libera hosts extras', async () => {
    ambiente = await montar({ env: { BAILEYS_HOSTS_WEBHOOK_PERMITIDOS: 'extra.local' } });
    const padrao = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://bot-java:8080/webhook/whatsapp' } });
    expect(padrao.status).toBe(200);
    const extra = await ambiente.chamar('POST', '/api/sessao_b/start-session', { corpo: { webhook: 'https://extra.local/hook' } });
    expect(extra.status).toBe(200);
    const fora = await ambiente.chamar('POST', '/api/sessao_c/start-session', { corpo: { webhook: 'http://java/webhook' } });
    expect(fora.status).toBe(400);
  });

  it('no boot, o meta.json com URL fora da lista e ignorado e a sessao usa o webhook padrao', async () => {
    ambiente = await montar({ env: { BAILEYS_HOSTS_WEBHOOK_PERMITIDOS: '' } });
    await ambiente.salvarCredenciais('sessao_a', 'http://atacante.exemplo/hook');
    expect(await ambiente.gerenciador.restaurar()).toBe(1);
    ambiente.sockets[0].emitir('connection.update', { connection: 'open' });
    await vi.waitFor(() => expect(ambiente!.postagens.length).toBeGreaterThan(0));
    expect(ambiente.postagens[0].url).toBe('http://bot-java:8080/webhook/whatsapp');
  });
});

describe('sessao pareada por QR e codigo', () => {
  it('credenciais de QR (registered=false, com me e account) restauram no boot, em INITIALIZING e visiveis no health', async () => {
    const { socket } = await restaurar();
    expect(await estadoDe('sessao_a')).toBe('INITIALIZING');
    expect(ambiente!.gerenciador.obter('sessao_a')!.status()).toEqual({ status: 'INITIALIZING', qrcode: null });
    const reconectando = await ambiente!.chamar('GET', '/api/health', { token: SEGREDO });
    expect(reconectando.json.sessoes.desconectadas).toEqual(['sessao_a']);
    expect(reconectando.json.sessoes.nomesConectadas).toEqual([]);
    socket.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.estado).toBe('CONNECTED'));
    const conectada = await ambiente!.chamar('GET', '/api/health', { token: SEGREDO });
    expect(conectada.json.sessoes.nomesConectadas).toEqual(['sessao_a']);
  });

  it('codigo pedido e nao digitado (me sem account) nao e sessao pareada: nao restaura e reconecta pedindo QR', async () => {
    ambiente = await montar();
    await ambiente.salvarCredenciais('sessao_a', undefined, CREDS_CODIGO_NAO_DIGITADO);
    expect(await ambiente.gerenciador.restaurar()).toBe(0);
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    expect(await estadoDe('sessao_a')).toBe('QRCODE');
    ambiente.sockets[0].emitir('connection.update', queda(408));
    await vi.waitFor(() => expect(ambiente!.agendados.length).toBeGreaterThan(0));
    expect(await estadoDe('sessao_a')).toBe('QRCODE');
  });
});

describe('ciclo de vida', () => {
  it('reconecta no boot a sessao com credenciais, sem o Java chamar start-session, e usa o webhook do meta.json', async () => {
    const { socket } = await restaurar();
    expect(await estadoDe('sessao_a')).toBe('INITIALIZING');
    expect(ambiente!.gerenciador.obter('sessao_a')!.status()).toEqual({ status: 'INITIALIZING', qrcode: null });

    socket.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(() => expect(ambiente!.postagens.length).toBeGreaterThan(0));
    expect(ambiente!.postagens[0].url).toBe('http://java-do-meta/webhook');
    expect(await estadoDe('sessao_a')).toBe('CONNECTED');
  });

  it('o start-session do Java no boot nao abre socket duplicado nem gera QR', async () => {
    await restaurar();
    const resposta = await ambiente!.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://java-do-meta/webhook', waitQrCode: false } });
    expect(resposta.json).toEqual({ status: 'INITIALIZING', qrcode: null });
    expect(ambiente!.sockets).toHaveLength(1);
    expect(await existe(join(ambiente!.pasta, 'sessao_a', 'creds.json'))).toBe(true);
  });

  it('sessao nunca pareada nao e restaurada no boot', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    await ambiente.fechar();
    ambiente = await montar({ env: { BAILEYS_AUTH_DIR: ambiente.pasta } });
    expect(await ambiente.gerenciador.restaurar()).toBe(0);
  });

  it('meta.json guarda o webhook da sessao', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://java/webhook' } });
    const meta = JSON.parse(await readFile(join(ambiente.pasta, 'sessao_a', 'meta.json'), 'utf8'));
    expect(meta).toEqual({ webhook: 'http://java/webhook' });
  });

  it('queda de rede ou servidor reconecta com espera crescente de 2 s ate 60 s, sem desistir, e mostra INITIALIZING', async () => {
    const { socket } = await restaurar();
    socket.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(async () => expect(await estadoDe('sessao_a')).toBe('CONNECTED'));

    const esperas: number[] = [];
    let atual = socket;
    const codigos = [428, 408, 503, 408, 428, 503, 408, 428];
    for (const codigo of codigos) {
      atual.emitir('connection.update', queda(codigo));
      await vi.waitFor(() => expect(ambiente!.agendados.filter((a) => !a.cancelado && a.ms >= 0).length).toBeGreaterThan(esperas.length));
      const agendado = ambiente!.agendados[ambiente!.agendados.length - 1];
      esperas.push(agendado.ms);
      expect(await estadoDe('sessao_a')).toBe('INITIALIZING');
      expect(ambiente!.gerenciador.obter('sessao_a')!.status().qrcode).toBeNull();
      const antes = ambiente!.sockets.length;
      agendado.fn();
      await vi.waitFor(() => expect(ambiente!.sockets.length).toBe(antes + 1));
      atual = ambiente!.sockets[ambiente!.sockets.length - 1];
    }
    expect(esperas).toEqual([2000, 4000, 8000, 16000, 32000, 60000, 60000, 60000]);

    atual.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(async () => expect(await estadoDe('sessao_a')).toBe('CONNECTED'));
    atual.emitir('connection.update', queda(428));
    await vi.waitFor(() => expect(ambiente!.agendados[ambiente!.agendados.length - 1].ms).toBe(2000));
  });

  it('restartRequired reconecta na hora', async () => {
    const { socket } = await restaurar();
    socket.emitir('connection.update', queda(515));
    await vi.waitFor(() => expect(ambiente!.agendados.length).toBeGreaterThan(0));
    expect(ambiente!.agendados[ambiente!.agendados.length - 1].ms).toBe(0);
    ambiente!.agendados[ambiente!.agendados.length - 1].fn();
    await vi.waitFor(() => expect(ambiente!.sockets).toHaveLength(2));
  });

  it('connectionReplaced nao reconecta em laco: fica DISCONNECTED e preserva as credenciais', async () => {
    const { socket } = await restaurar();
    socket.emitir('connection.update', queda(440));
    await vi.waitFor(async () => expect(await estadoDe('sessao_a')).toBe('DISCONNECTED'));
    expect(ambiente!.agendados).toHaveLength(0);
    expect(ambiente!.sockets).toHaveLength(1);
    expect(await existe(join(ambiente!.pasta, 'sessao_a', 'creds.json'))).toBe(true);
  });

  it.each([401, 500, 411])('codigo %i apaga as credenciais, vai a DISCONNECTED e o proximo start-session gera QR novo', async (codigo) => {
    const { socket } = await restaurar();
    socket.emitir('connection.update', { connection: 'open' });
    await vi.waitFor(async () => expect(await estadoDe('sessao_a')).toBe('CONNECTED'));
    socket.emitir('connection.update', queda(codigo));
    await vi.waitFor(async () => expect(await estadoDe('sessao_a')).toBe('DISCONNECTED'));
    expect(ambiente!.agendados).toHaveLength(0);
    expect(await existe(join(ambiente!.pasta, 'sessao_a'))).toBe(false);
    expect(ambiente!.postagens.some((p) => p.payload.status === 'desconnectedMobile')).toBe(true);

    const resposta = await ambiente!.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://java/webhook' } });
    expect(resposta.json.status).toBe('QRCODE');
    expect(ambiente!.sockets).toHaveLength(2);
  });

  it('aguardando QR, a queda reconecta mostrando QRCODE (nunca INITIALIZING) e renova o QR sem desistir', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@um' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    const primeiro = ambiente.gerenciador.obter('sessao_a')!.status().qrcode;

    ambiente.sockets[0].emitir('connection.update', queda(408));
    await vi.waitFor(() => expect(ambiente!.agendados).toHaveLength(1));
    expect(ambiente.gerenciador.obter('sessao_a')!.status().status).toBe('QRCODE');
    ambiente.agendados[0].fn();
    await vi.waitFor(() => expect(ambiente!.sockets).toHaveLength(2));
    ambiente.sockets[1].emitir('connection.update', { qr: '2@dois' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    expect(ambiente.gerenciador.obter('sessao_a')!.status().qrcode).not.toBe(primeiro);
    expect(ambiente.postagens.filter((p) => p.payload.event === 'qrcode')).toHaveLength(2);
  });

  it('start-session durante a espera de reconexao nao abre outro socket', async () => {
    const { socket } = await restaurar();
    socket.emitir('connection.update', queda(408));
    await vi.waitFor(() => expect(ambiente!.agendados).toHaveLength(1));
    const resposta = await ambiente!.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://java-do-meta/webhook' } });
    expect(resposta.json.status).toBe('INITIALIZING');
    expect(ambiente!.sockets).toHaveLength(1);
  });

  it('falha ao abrir o socket cai na reconexao com espera', async () => {
    ambiente = await montar({
      deps: {
        fabrica: async () => {
          throw new Error('sem rede');
        }
      }
    });
    await ambiente.salvarCredenciais('sessao_a');
    await ambiente.gerenciador.restaurar();
    await vi.waitFor(() => expect(ambiente!.agendados).toHaveLength(1));
    expect(ambiente.agendados[0].ms).toBe(2000);
    expect(await estadoDe('sessao_a')).toBe('INITIALIZING');
  });
});

describe('envio aceito com o socket caido', () => {
  it('tenta de novo quando a sessao reconecta em ate 60 s', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    socket.falhaDeEnvio = Object.assign(new Error('Connection Closed'), { output: { statusCode: 428 } });
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '123456789012345', isLid: true, message: 'oi' } });
    expect(resposta.status).toBe(200);

    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')!.fila.tamanho).toBe(1));
    socket.emitir('connection.update', queda(428));
    await vi.waitFor(() => expect(ambiente!.agendados.length).toBeGreaterThan(0));
    const reabrir = ambiente.agendados.find((a) => a.ms === 2000)!;
    reabrir.fn();
    await vi.waitFor(() => expect(ambiente!.sockets).toHaveLength(2));
    ambiente.sockets[1].emitir('connection.update', { connection: 'open' });

    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(ambiente.sockets[1].enviados.map((e) => e.conteudo.text)).toEqual(['oi']);
  });

  it('descarta o envio se a sessao nao reconectar em 60 s', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    socket.falhaDeEnvio = Object.assign(new Error('Connection Closed'), { output: { statusCode: 428 } });
    await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '123456789012345', isLid: true, message: 'oi' } });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')!.fila.tamanho).toBe(1));
    socket.emitir('connection.update', queda(428));
    await vi.waitFor(() => expect(ambiente!.agendados.length).toBeGreaterThan(0));
    const limite = ambiente.agendados.find((a) => a.ms === 60000);
    expect(limite).toBeDefined();
    limite!.fn();
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados).toHaveLength(0);
    expect(ambiente.sockets).toHaveLength(1);
  });
});

describe('logs do gateway (WPP-06)', () => {
  it('recebimento, envio, QR e erro nao deixam numero completo, texto, token, segredo nem QR no log', async () => {
    const linhas: string[] = [];
    const logger = criarLogger('debug', { write: (linha: string) => void linhas.push(linha) } as never);
    ambiente = await montar({ logger });
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://java/webhook' } });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@TEXTOCRUDOQR,abc,def' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    const qr = ambiente.gerenciador.obter('sessao_a')!.status().qrcode as string;
    const socket = await ambiente.conectar('sessao_a');

    socket.emitir('messages.upsert', {
      type: 'notify',
      messages: [mensagemTexto({ key: { remoteJid: '5511955553333@s.whatsapp.net', fromMe: false, id: 'LOG1' }, message: { conversation: 'Quero alugar o salao para sabado' } })]
    });
    socket.emitir('messages.upsert', {
      type: 'notify',
      messages: [mensagemTexto({ key: { remoteJid: '5511955553333@s.whatsapp.net', fromMe: true, id: 'LOG2' }, message: { conversation: 'Resposta secreta do dono' } })]
    });
    socket.existentes['5511955553333@s.whatsapp.net'] = '5511955553333@s.whatsapp.net';
    await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511955553333', message: 'Texto enviado pelo bot' } });
    socket.falhaDeEnvio = new Error('falha com 5511955553333@s.whatsapp.net');
    await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511955553333', message: 'Outro texto do bot' } });
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    await vi.waitFor(() => expect(ambiente!.postagens.length).toBeGreaterThan(2));

    const saida = linhas.join(' | ');
    expect(saida).toContain('***3333');
    for (const proibido of [
      '5511955553333',
      'Quero alugar o salao',
      'Resposta secreta do dono',
      'Texto enviado pelo bot',
      'Outro texto do bot',
      'TEXTOCRUDOQR',
      qr.slice(30, 80),
      SEGREDO,
      SEGREDO_WEBHOOK,
      ambiente.token('sessao_a')
    ]) {
      expect(saida, proibido).not.toContain(proibido);
    }
  });
});
