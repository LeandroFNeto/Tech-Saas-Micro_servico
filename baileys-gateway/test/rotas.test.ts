import { access } from 'node:fs/promises';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { gerarToken } from '../src/seguranca.js';
import { montar, SEGREDO, type Ambiente } from './helpers.js';

let ambiente: Ambiente | undefined;

afterEach(async () => {
  await ambiente?.fechar();
  ambiente = undefined;
});

const PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

async function existe(caminho: string): Promise<boolean> {
  try {
    await access(caminho);
    return true;
  } catch {
    return false;
  }
}

describe('token e autenticacao', () => {
  it('generate-token devolve status e token; segredo errado responde 401', async () => {
    ambiente = await montar();
    const ok = await ambiente.chamar('POST', `/api/sessao_a/${SEGREDO}/generate-token`, { token: null });
    expect(ok.status).toBe(200);
    expect(ok.json.status).toBe('success');
    expect(ok.json.token).toBe(gerarToken(SEGREDO, 'sessao_a'));

    const errado = await ambiente.chamar('POST', '/api/sessao_a/segredo-errado/generate-token', { token: null });
    expect(errado.status).toBe(401);
    expect(errado.json.token).toBeUndefined();
  });

  it('nome de sessao com caminho e recusado', async () => {
    ambiente = await montar();
    const resposta = await ambiente.chamar('POST', `/api/..%2Fx/${SEGREDO}/generate-token`, { token: null });
    expect([400, 404]).toContain(resposta.status);
    const ponto = await ambiente.chamar('GET', '/api/sessao%20com%20espaco/status-session', { token: null });
    expect(ponto.status).toBe(400);
  });

  it('o token continua valido depois do reinicio do gateway e nao vale para outra sessao', async () => {
    ambiente = await montar();
    const token = (await ambiente.chamar('POST', `/api/sessao_a/${SEGREDO}/generate-token`, { token: null })).json.token;
    await ambiente.fechar();

    ambiente = await montar();
    const mesmo = await ambiente.chamar('GET', '/api/sessao_a/status-session', { token });
    expect(mesmo.status).toBe(200);
    const outra = await ambiente.chamar('GET', '/api/sessao_b/status-session', { token });
    expect(outra.status).toBe(401);
  });

  it('todas as rotas da sessao exigem Bearer: ausente, invalido ou de outra sessao responde 401', async () => {
    ambiente = await montar();
    const rotas: Array<[string, string]> = [
      ['POST', '/api/sessao_a/start-session'],
      ['GET', '/api/sessao_a/status-session'],
      ['GET', '/api/sessao_a/qrcode-session'],
      ['POST', '/api/sessao_a/send-message'],
      ['POST', '/api/sessao_a/send-image'],
      ['POST', '/api/sessao_a/close-session'],
      ['POST', '/api/sessao_a/request-pairing-code']
    ];
    for (const [metodo, caminho] of rotas) {
      expect((await ambiente.chamar(metodo, caminho, { token: null, corpo: {} })).status, `${caminho} sem token`).toBe(401);
      expect((await ambiente.chamar(metodo, caminho, { token: 'lixo', corpo: {} })).status, `${caminho} token invalido`).toBe(401);
      expect((await ambiente.chamar(metodo, caminho, { token: ambiente.token('sessao_b'), corpo: {} })).status, `${caminho} outra sessao`).toBe(401);
    }
    expect(ambiente.sockets).toHaveLength(0);
  });

  it('corpo invalido responde 400 e rota inexistente 404, sem detalhe interno', async () => {
    ambiente = await montar();
    const ruim = await ambiente.chamar('POST', '/api/sessao_a/send-message', { bruto: '{nao-e-json' });
    expect(ruim.status).toBe(400);
    const desconhecida = await ambiente.chamar('GET', '/nada', { token: null });
    expect(desconhecida.status).toBe(404);
  });
});

describe('start-session, status e QR', () => {
  it('start-session repetido nao abre outro socket e devolve o QR atual', async () => {
    ambiente = await montar();
    const primeira = await ambiente.chamar('POST', '/api/sessao_a/start-session', {
      corpo: { webhook: 'http://bot-java:8080/webhook/whatsapp', waitQrCode: false }
    });
    expect(primeira.status).toBe(200);
    expect(primeira.json).toEqual({ status: 'QRCODE', qrcode: null });
    expect(ambiente.sockets).toHaveLength(1);

    ambiente.sockets[0].emitir('connection.update', { qr: '2@textoCruDoQr,abc,def' });
    await vi.waitFor(async () => {
      expect((await ambiente!.chamar('GET', '/api/sessao_a/status-session')).json.qrcode).not.toBeNull();
    });

    const segunda = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://bot-java:8080/webhook/whatsapp' } });
    expect(ambiente.sockets).toHaveLength(1);
    expect(segunda.json.status).toBe('QRCODE');
    expect(segunda.json.qrcode).toMatch(/^data:image\/png;base64,/);
  });

  it('start-session de sessao conectada devolve CONNECTED sem novo socket', async () => {
    ambiente = await montar();
    await ambiente.conectar('sessao_a');
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://bot-java:8080/webhook/whatsapp' } });
    expect(resposta.json).toEqual({ status: 'CONNECTED', qrcode: null });
    expect(ambiente.sockets).toHaveLength(1);
  });

  it('webhook invalido no start-session responde 400', async () => {
    ambiente = await montar();
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'javascript:alert(1)' } });
    expect(resposta.status).toBe(400);
    expect(ambiente.sockets).toHaveLength(0);
  });

  it('QR e sempre PNG em base64: no status, nos bytes do qrcode-session e no evento qrcode do webhook', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: { webhook: 'http://destino/web' } });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@textoCruDoQr,abc,def' });
    await vi.waitFor(() => expect(ambiente!.postagens.length).toBeGreaterThan(0));

    const status = await ambiente.chamar('GET', '/api/sessao_a/status-session');
    expect(status.json.status).toBe('QRCODE');
    expect(status.json.qrcode).toMatch(/^data:image\/png;base64,/);
    expect(status.json.qrcode).not.toContain('2@textoCruDoQr');

    const bytes = await ambiente.chamar('GET', '/api/sessao_a/qrcode-session');
    expect(bytes.tipo).toContain('image/png');
    expect(bytes.bytes.subarray(0, 8)).toEqual(PNG);

    const evento = ambiente.postagens[0];
    expect(evento.url).toBe('http://destino/web');
    expect(evento.payload.event).toBe('qrcode');
    expect(evento.payload.session).toBe('sessao_a');
    expect(evento.payload.qrcode).toMatch(/^data:image\/png;base64,/);
    expect(JSON.stringify(evento.payload)).not.toContain('textoCruDoQr');
  });

  it('sem QR, qrcode-session responde JSON e sessao desconhecida fica DISCONNECTED', async () => {
    ambiente = await montar();
    const semQr = await ambiente.chamar('GET', '/api/sessao_a/qrcode-session');
    expect(semQr.tipo).toContain('json');
    expect(semQr.json.qrcode).toBeNull();
    const status = await ambiente.chamar('GET', '/api/sessao_a/status-session');
    expect(status.json).toEqual({ status: 'DISCONNECTED', qrcode: null });
  });

  it('ler o QR comuta o status para CONNECTED e emite status-find', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    ambiente.sockets[0].emitir('connection.update', { connection: 'open' });
    await vi.waitFor(async () => {
      expect((await ambiente!.chamar('GET', '/api/sessao_a/status-session')).json).toEqual({ status: 'CONNECTED', qrcode: null });
    });
    expect(ambiente.postagens.some((p) => p.payload.event === 'status-find' && p.payload.status === 'inChat')).toBe(true);
  });
});

describe('send-message', () => {
  it('sessao desconectada responde 404', async () => {
    ambiente = await montar();
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511988887777', message: 'oi', isGroup: false, isLid: false } });
    expect(resposta.status).toBe(404);
  });

  it('numero inexistente responde 400; numero com ou sem nono digito usa o JID real do WhatsApp', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    socket.existentes['551188887777@s.whatsapp.net'] = '5511988887777@s.whatsapp.net';

    const inexistente = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511977776666', message: 'oi', isGroup: false, isLid: false } });
    expect(inexistente.status).toBe(400);

    const aceito = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '551188887777', message: 'oi', isGroup: false, isLid: false } });
    expect(aceito.status).toBe(200);
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados).toHaveLength(1);
    expect(socket.enviados[0].jid).toBe('5511988887777@s.whatsapp.net');
    expect(socket.enviados[0].conteudo).toEqual({ text: 'oi' });
  });

  it('destino @lid por isLid, por sufixo ou por 14 digitos nao confere o numero', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    const espiao = vi.spyOn(socket, 'onWhatsApp');
    for (const phone of ['123456789012345', '123456789012345@lid', '12345678901234']) {
      const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-message', {
        corpo: { phone, message: 'oi', isGroup: false, isLid: phone === '123456789012345' }
      });
      expect(resposta.status).toBe(200);
    }
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(espiao).not.toHaveBeenCalled();
    expect(socket.enviados.map((e) => e.jid)).toEqual(['123456789012345@lid', '123456789012345@lid', '12345678901234@lid']);
  });

  it('valida corpo: mensagem vazia, longa demais, numero invalido e grupo', async () => {
    ambiente = await montar();
    await ambiente.conectar('sessao_a');
    const chamar = (corpo: unknown) => ambiente!.chamar('POST', '/api/sessao_a/send-message', { corpo });
    expect((await chamar({ phone: '5511988887777', message: '' })).status).toBe(400);
    expect((await chamar({ phone: '5511988887777', message: 'x'.repeat(10001) })).status).toBe(400);
    expect((await chamar({ phone: 'abc', message: 'oi' })).status).toBe(400);
    expect((await chamar({ phone: '5511988887777', message: 'oi', isGroup: true })).status).toBe(400);
    expect((await chamar({ message: 'oi' })).status).toBe(400);
  });

  it('responde antes de digitar e enviar; depois envia com digitando e o id registrado para o eco', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    let liberar: () => void = () => undefined;
    socket.portaoEnvio = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    socket.existentes['5511988887777@s.whatsapp.net'] = '5511988887777@s.whatsapp.net';

    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511988887777', message: 'Olá! Como posso ajudar?' } });
    expect(resposta.status).toBe(200);
    expect(socket.enviados).toHaveLength(0);

    liberar();
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados).toHaveLength(1);
    expect(socket.presencas.map((p) => p[0])).toEqual(['composing', 'paused']);
    expect(socket.enviados[0].opcoes.messageId).toMatch(/^3EB0[0-9A-F]{32}$/);
  });

  it('digitando dura 40 ms por caractere, entre 0,8 s e 3 s, e pode ser desligado', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    socket.existentes['5511988887777@s.whatsapp.net'] = '5511988887777@s.whatsapp.net';
    const enviar = (message: string) => ambiente!.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '5511988887777', message } });
    await enviar('oi');
    await enviar('x'.repeat(40));
    await enviar('y'.repeat(500));
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    const digitacoes = ambiente.dormidas.filter((ms) => [800, 1600, 3000].includes(ms));
    expect(digitacoes).toEqual([800, 1600, 3000]);
    await ambiente.fechar();

    ambiente = await montar({ env: { BAILEYS_SIMULAR_DIGITACAO: 'false' } });
    const sem = await ambiente.conectar('sessao_b');
    sem.existentes['5511988887777@s.whatsapp.net'] = '5511988887777@s.whatsapp.net';
    await ambiente.chamar('POST', '/api/sessao_b/send-message', { corpo: { phone: '5511988887777', message: 'oi' } });
    await ambiente.gerenciador.obter('sessao_b')!.fila.aguardarOciosa();
    expect(sem.presencas).toEqual([]);
    expect(sem.enviados).toHaveLength(1);
  });

  it('envios da mesma sessao saem na ordem de chegada e o envio 51 da fila recebe 503', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    let liberar: () => void = () => undefined;
    socket.portaoEnvio = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    const status: number[] = [];
    for (let i = 0; i < 51; i++) {
      const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '123456789012345', isLid: true, message: `m${i}` } });
      status.push(resposta.status);
    }
    expect(status.filter((s) => s === 200)).toHaveLength(50);
    expect(status[50]).toBe(503);
    liberar();
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados.map((e) => e.conteudo.text)).toEqual(Array.from({ length: 50 }, (_, i) => `m${i}`));
  });

  it('erro de envio que nao e de conexao e descartado sem derrubar a fila', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a');
    socket.falhaDeEnvio = new Error('falha qualquer');
    await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '123456789012345', isLid: true, message: 'a' } });
    await ambiente.chamar('POST', '/api/sessao_a/send-message', { corpo: { phone: '123456789012345', isLid: true, message: 'b' } });
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados.map((e) => e.conteudo.text)).toEqual(['b']);
  });
});

describe('send-image', () => {
  const fotoOk = () => vi.fn(async () => ({ status: 200, contentType: 'image/jpeg', contentLength: 12, corpo: Buffer.from('imagem-bytes') }));
  const publico = async () => ['93.184.216.34'];

  it('baixa a URL cadastrada e envia como imagem com a legenda', async () => {
    const requisitar = fotoOk();
    ambiente = await montar({ download: { requisitar, resolverDns: publico } });
    const socket = await ambiente.conectar('sessao_a');
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-image', {
      corpo: { phone: '123456789012345', isLid: true, isGroup: false, path: 'https://fotos.exemplo.com/salao.jpg', filename: 'salao.jpg', caption: 'Nosso salão' }
    });
    expect(resposta.status).toBe(200);
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados).toHaveLength(1);
    expect(socket.enviados[0].jid).toBe('123456789012345@lid');
    expect(Buffer.from(socket.enviados[0].conteudo.image).toString()).toBe('imagem-bytes');
    expect(socket.enviados[0].conteudo.caption).toBe('Nosso salão');
    expect(socket.enviados[0].conteudo.mimetype).toBe('image/jpeg');
    expect(socket.presencas.map((p) => p[0])).toEqual(['composing', 'paused']);
    expect(ambiente.dormidas).toContain(1000);
  });

  it('aceita data URL base64', async () => {
    ambiente = await montar({ download: { resolverDns: publico } });
    const socket = await ambiente.conectar('sessao_a');
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-image', {
      corpo: { phone: '123456789012345', isLid: true, path: `data:image/png;base64,${Buffer.from('png').toString('base64')}`, filename: 'a.png' }
    });
    expect(resposta.status).toBe(200);
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados[0].conteudo.caption).toBeUndefined();
  });

  it('falha no download responde erro HTTP antes de entrar na fila; sessao desconectada responde 404', async () => {
    const requisitar = vi.fn(async () => ({ status: 404, contentType: 'text/plain', contentLength: 1, corpo: Buffer.from('x') }));
    ambiente = await montar({ download: { requisitar, resolverDns: publico } });
    const desconectada = await ambiente.chamar('POST', '/api/sessao_a/send-image', { corpo: { phone: '123456789012345', isLid: true, path: 'https://x.com/a.jpg' } });
    expect(desconectada.status).toBe(404);

    const socket = await ambiente.conectar('sessao_a');
    const falha = await ambiente.chamar('POST', '/api/sessao_a/send-image', { corpo: { phone: '123456789012345', isLid: true, path: 'https://x.com/a.jpg' } });
    expect(falha.status).toBe(502);
    const interna = await ambiente.chamar('POST', '/api/sessao_a/send-image', { corpo: { phone: '123456789012345', isLid: true, path: 'http://127.0.0.1/a.jpg' } });
    expect(interna.status).toBe(400);
    const semPath = await ambiente.chamar('POST', '/api/sessao_a/send-image', { corpo: { phone: '123456789012345', isLid: true } });
    expect(semPath.status).toBe(400);
    await ambiente.gerenciador.obter('sessao_a')!.fila.aguardarOciosa();
    expect(socket.enviados).toHaveLength(0);
  });

  it('numero inexistente responde 400 na foto, como no texto', async () => {
    ambiente = await montar({ download: { requisitar: fotoOk(), resolverDns: publico } });
    await ambiente.conectar('sessao_a');
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/send-image', { corpo: { phone: '5511977776666', path: 'https://x.com/a.jpg' } });
    expect(resposta.status).toBe(400);
  });
});

describe('close-session', () => {
  it('encerra o socket, faz logout e apaga as credenciais; a sessao nao volta no boot seguinte', async () => {
    ambiente = await montar();
    const socket = await ambiente.conectar('sessao_a', 'http://bot-java:8080/webhook/whatsapp');
    const pasta = join(ambiente.pasta, 'sessao_a');
    expect(await existe(pasta)).toBe(true);

    const resposta = await ambiente.chamar('POST', '/api/sessao_a/close-session');
    expect(resposta.status).toBe(200);
    expect(socket.logouts).toBe(1);
    expect(socket.encerrado).toBeGreaterThanOrEqual(1);
    expect(await existe(pasta)).toBe(false);
    expect((await ambiente.chamar('GET', '/api/sessao_a/status-session')).json.status).toBe('DISCONNECTED');
    expect(await ambiente.gerenciador.restaurar()).toBe(0);
  });

  it('apaga a pasta mesmo de sessao que o processo nao conhece', async () => {
    ambiente = await montar();
    await ambiente.salvarCredenciais('sessao_x');
    expect((await ambiente.chamar('POST', '/api/sessao_x/close-session')).status).toBe(200);
    expect(await existe(join(ambiente.pasta, 'sessao_x'))).toBe(false);
  });
});

describe('saude', () => {
  it('exige o segredo e devolve so contagens e nomes de sessao', async () => {
    ambiente = await montar();
    await ambiente.conectar('sessao_a');
    await ambiente.chamar('POST', '/api/sessao_b/start-session', { corpo: {} });
    await ambiente.salvarCredenciais('sessao_c');
    await ambiente.chamar('POST', '/api/sessao_c/start-session', { corpo: {} });
    ambiente.sockets[2].emitir('connection.update', { connection: 'close', lastDisconnect: { error: { output: { statusCode: 440 } } } });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_c')?.estado).toBe('DISCONNECTED'));

    const recusada = await ambiente.chamar('GET', '/api/health', { token: 'errado' });
    expect(recusada.status).toBe(401);
    const semCabecalho = await ambiente.chamar('GET', '/api/health', { token: null });
    expect(semCabecalho.status).toBe(401);
    const legada = await ambiente.chamar('GET', `/api/${SEGREDO}/health`, { token: null });
    expect(legada.status).toBe(404);
    const ok = await ambiente.chamar('GET', '/api/health', { token: SEGREDO });
    expect(ok.status).toBe(200);
    expect(ok.json).toEqual({ status: 'ok', sessoes: { conectadas: 1, aguardandoQr: 1, desconectadas: ['sessao_c'], nomesConectadas: ['sessao_a'] } });
  });
});

describe('pareamento por codigo', () => {
  it('gera o codigo no formato ABCD-EFGH quando a sessao aguarda pareamento', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    const resposta = await ambiente.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } });
    expect(resposta.status).toBe(200);
    expect(resposta.json).toEqual({ code: 'ABCD-1234' });
  });

  it('sessao conectada responde 409; sessao desconhecida ou sem socket tambem', async () => {
    ambiente = await montar();
    await ambiente.conectar('sessao_a');
    expect((await ambiente.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } })).status).toBe(409);
    expect((await ambiente.chamar('POST', '/api/sessao_z/request-pairing-code', { corpo: { phone: '5511999990000' } })).status).toBe(409);
  });

  it('telefone invalido responde 400', async () => {
    ambiente = await montar();
    for (const phone of ['abc', '123', '', '5511999990000x']) {
      expect((await ambiente.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone } })).status).toBe(400);
    }
  });

  it('mais de 3 pedidos em 10 minutos na mesma sessao responde 429, e a janela libera depois', async () => {
    let agora = 1_000_000;
    ambiente = await montar({ deps: { agora: () => agora } });
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    const pedir = () => ambiente!.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } });
    expect((await pedir()).status).toBe(200);
    expect((await pedir()).status).toBe(200);
    expect((await pedir()).status).toBe(200);
    expect((await pedir()).status).toBe(429);
    agora += 10 * 60 * 1000 + 1;
    expect((await pedir()).status).toBe(200);
  });

  it('pedidos simultaneos respeitam o limite de 3', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    const respostas = await Promise.all(
      Array.from({ length: 8 }, () => ambiente!.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } }))
    );
    const status = respostas.map((r) => r.status);
    expect(status.filter((x) => x === 200)).toHaveLength(3);
    expect(status.filter((x) => x === 429)).toHaveLength(5);
  });

  it('falha 502 do Baileys consome o limite e 409 nao consome', async () => {
    ambiente = await montar();
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    const pedir = () => ambiente!.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } });
    await ambiente.conectar('sessao_b');
    for (let i = 0; i < 5; i++) {
      expect((await ambiente.chamar('POST', '/api/sessao_b/request-pairing-code', { corpo: { phone: '5511999990000' } })).status).toBe(409);
    }
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    ambiente.sockets[0].falhaDePareamento = new Error('falha');
    for (let i = 0; i < 3; i++) {
      expect((await pedir()).status).toBe(502);
    }
    expect((await pedir()).status).toBe(429);
  });

  it('409 de sessao ainda iniciando desfaz o registro e nao consome o limite', async () => {
    ambiente = await montar({
      deps: { agendar: (acao: () => void, ms: number) => (ms === 3000 ? setTimeout(acao, 0) : 0) }
    });
    await ambiente.chamar('POST', '/api/sessao_a/start-session', { corpo: {} });
    const pedir = () => ambiente!.chamar('POST', '/api/sessao_a/request-pairing-code', { corpo: { phone: '5511999990000' } });
    for (let i = 0; i < 5; i++) {
      const resposta = await pedir();
      expect(resposta.status).toBe(409);
    }
    ambiente.sockets[0].emitir('connection.update', { qr: '2@abc' });
    await vi.waitFor(() => expect(ambiente!.gerenciador.obter('sessao_a')?.status().qrcode).not.toBeNull());
    expect((await pedir()).status).toBe(200);
  });
});
