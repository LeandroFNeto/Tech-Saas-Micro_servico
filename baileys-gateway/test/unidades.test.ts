import { mkdtemp, readdir, readFile, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it, vi } from 'vitest';
import { gravarAtomico, lerComReserva } from '../src/arquivos.js';
import { credenciaisPareadas, usarAutenticacaoAtomica } from '../src/auth-state.js';
import { carregarConfig, ErroDeConfiguracao, webhookPermitido } from '../src/config.js';
import { FilaDeEnvio } from '../src/fila.js';
import { criarLogger } from '../src/logger.js';
import { mascararNumero, sanear } from '../src/mascara.js';
import { baixarImagem, enderecoPrivado, ErroDeMidia, requisitarNoIpFixo } from '../src/midia.js';
import { IdsRecentes, MapaRecente } from '../src/recentes.js';
import { extrairBearer, gerarToken, igualSeguro, sessaoValida, tokenValido } from '../src/seguranca.js';
import { DespachanteWebhook } from '../src/webhook.js';

const log = criarLogger('silent');

describe('config', () => {
  it('nao sobe sem SECRET_KEY nem sem segredo do webhook', () => {
    expect(() => carregarConfig({})).toThrow(ErroDeConfiguracao);
    expect(() => carregarConfig({ SECRET_KEY: '   ' })).toThrow(ErroDeConfiguracao);
  });

  it('segredo do webhook cai para SECRET_KEY e os padroes batem com o fork', () => {
    const config = carregarConfig({ SECRET_KEY: 'abc' });
    expect(config.segredoWebhook).toBe('abc');
    expect(config.aoMensagemDoDono).toBe(true);
    expect(config.simularDigitacao).toBe(true);
    expect(config.urlWebhookPadrao).toBe('http://bot-java:8080/webhook/whatsapp');
    expect(config.porta).toBe(21465);
    expect(config.pastaAutenticacao).toBe('/auth-data');
  });

  it('webhook aceita so o host padrao mais a lista opcional', () => {
    const padrao = carregarConfig({ SECRET_KEY: 'abc' });
    expect(padrao.hostsWebhookPermitidos).toEqual(['bot-java']);
    expect(webhookPermitido('http://bot-java:8080/webhook/whatsapp', padrao.hostsWebhookPermitidos)).toBe(true);
    expect(webhookPermitido('http://BOT-JAVA/outro', padrao.hostsWebhookPermitidos)).toBe(true);
    expect(webhookPermitido('http://atacante.com/x', padrao.hostsWebhookPermitidos)).toBe(false);
    expect(webhookPermitido('http://169.254.169.254/x', padrao.hostsWebhookPermitidos)).toBe(false);
    expect(webhookPermitido('ftp://bot-java/x', padrao.hostsWebhookPermitidos)).toBe(false);
    expect(webhookPermitido('nao-e-url', padrao.hostsWebhookPermitidos)).toBe(false);
    const comLista = carregarConfig({ SECRET_KEY: 'abc', WPPCONNECT_WEBHOOK_URL: 'http://reserva:9/w', BAILEYS_HOSTS_WEBHOOK_PERMITIDOS: ' Extra.local , outro ' });
    expect(comLista.hostsWebhookPermitidos).toEqual(['reserva', 'extra.local', 'outro']);
    expect(webhookPermitido('https://extra.local/x', comLista.hostsWebhookPermitidos)).toBe(true);
  });

  it('variaveis de comportamento desligam recursos', () => {
    const config = carregarConfig({
      SECRET_KEY: 'abc',
      WPPCONNECT_WEBHOOK_SECRET: 'def',
      WPPCONNECT_ON_SELF_MESSAGE: 'false',
      BAILEYS_SIMULAR_DIGITACAO: 'false'
    });
    expect(config.segredoWebhook).toBe('def');
    expect(config.aoMensagemDoDono).toBe(false);
    expect(config.simularDigitacao).toBe(false);
  });

  it('rejeita URL de webhook que nao e http', () => {
    expect(() => carregarConfig({ SECRET_KEY: 'abc', WPPCONNECT_WEBHOOK_URL: 'file:///etc/passwd' })).toThrow(ErroDeConfiguracao);
  });
});

describe('seguranca', () => {
  it('token e HMAC-SHA256 do segredo e da sessao, sem estado', () => {
    const token = gerarToken('segredo', 'sessao_a');
    expect(token).toMatch(/^[0-9a-f]{64}$/);
    expect(gerarToken('segredo', 'sessao_a')).toBe(token);
    expect(tokenValido('segredo', 'sessao_a', token)).toBe(true);
    expect(tokenValido('segredo', 'sessao_b', token)).toBe(false);
    expect(tokenValido('outro', 'sessao_a', token)).toBe(false);
    expect(tokenValido('segredo', 'sessao_a', undefined)).toBe(false);
    expect(tokenValido('segredo', 'sessao_a', '')).toBe(false);
  });

  it('comparacao de segredo funciona com tamanhos diferentes', () => {
    expect(igualSeguro('abc', 'abc')).toBe(true);
    expect(igualSeguro('abc', 'abcd')).toBe(false);
    expect(igualSeguro('', 'x')).toBe(false);
  });

  it('extrai o Bearer', () => {
    expect(extrairBearer('Bearer xyz')).toBe('xyz');
    expect(extrairBearer('bearer xyz')).toBe('xyz');
    expect(extrairBearer('Basic xyz')).toBeUndefined();
    expect(extrairBearer('Bearer')).toBeUndefined();
    expect(extrairBearer(undefined)).toBeUndefined();
  });

  it('nome de sessao nao permite caminho', () => {
    expect(sessaoValida('sessao_recanto_01')).toBe(true);
    expect(sessaoValida('teste-sessao')).toBe(true);
    expect(sessaoValida('..')).toBe(false);
    expect(sessaoValida('../x')).toBe(false);
    expect(sessaoValida('a/b')).toBe(false);
    expect(sessaoValida('')).toBe(false);
  });
});

describe('webhook', () => {
  const resposta = (status: number) => ({ ok: status >= 200 && status < 300, status }) as Response;

  function criar(buscar: ReturnType<typeof vi.fn>, esperas: number[] = []) {
    const dormir = vi.fn(async (ms: number) => {
      esperas.push(ms);
    });
    return { despachante: new DespachanteWebhook({ segredo: 'seg-webhook', buscar: buscar as never, dormir }, log), esperas };
  }

  it('envia o cabecalho x-webhook-token em toda chamada', async () => {
    const buscar = vi.fn(async () => resposta(200));
    const { despachante } = criar(buscar);
    const resultado = await despachante.postar('http://bot-java:8080/webhook/whatsapp', 's1', { a: 1 });
    expect(resultado).toEqual({ entregue: true, tentativas: 1 });
    const [url, opcoes] = buscar.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('http://bot-java:8080/webhook/whatsapp');
    expect((opcoes.headers as Record<string, string>)['x-webhook-token']).toBe('seg-webhook');
    expect(opcoes.body).toBe('{"a":1}');
  });

  it('tenta mais 3 vezes (1 s, 2 s, 4 s) com 5xx e entrega na ultima', async () => {
    const buscar = vi.fn().mockResolvedValueOnce(resposta(500)).mockResolvedValueOnce(resposta(503)).mockResolvedValueOnce(resposta(502)).mockResolvedValueOnce(resposta(200));
    const { despachante, esperas } = criar(buscar);
    const resultado = await despachante.postar('http://x/y', 's1', {});
    expect(resultado).toEqual({ entregue: true, tentativas: 4 });
    expect(esperas).toEqual([1000, 2000, 4000]);
  });

  it('desiste depois de 3 novas tentativas em erro de rede', async () => {
    const buscar = vi.fn().mockRejectedValue(new TypeError('fetch failed'));
    const { despachante, esperas } = criar(buscar);
    const resultado = await despachante.postar('http://x/y', 's1', {});
    expect(resultado.entregue).toBe(false);
    expect(buscar).toHaveBeenCalledTimes(4);
    expect(esperas).toEqual([1000, 2000, 4000]);
  });

  it('401 e outros 4xx nao repetem', async () => {
    for (const status of [400, 401, 403, 404]) {
      const buscar = vi.fn(async () => resposta(status));
      const { despachante, esperas } = criar(buscar);
      const resultado = await despachante.postar('http://x/y', 's1', {});
      expect(resultado.entregue).toBe(false);
      expect(buscar).toHaveBeenCalledTimes(1);
      expect(esperas).toEqual([]);
    }
  });
});

describe('ids recentes', () => {
  it('descarta repetidos dentro de 10 min e libera depois', () => {
    let agora = 0;
    const ids = new IdsRecentes(10 * 60 * 1000, 5000, () => agora);
    expect(ids.registrarSeNovo('a')).toBe(true);
    expect(ids.registrarSeNovo('a')).toBe(false);
    agora = 9 * 60 * 1000;
    expect(ids.registrarSeNovo('a')).toBe(false);
    agora = 10 * 60 * 1000 + 9 * 60 * 1000 + 1;
    expect(ids.registrarSeNovo('a')).toBe(true);
  });

  it('guarda no maximo 5000 ids', () => {
    const ids = new IdsRecentes(60000, 5000, () => 0);
    for (let i = 0; i < 6000; i++) {
      ids.adicionar(`id${i}`);
    }
    expect(ids.tamanho).toBe(5000);
    expect(ids.contem('id0')).toBe(false);
    expect(ids.contem('id5999')).toBe(true);
  });

  it('ids enviados pelo gateway valem muito alem da janela de 2 min do Java', () => {
    let agora = 0;
    const enviados = new IdsRecentes(24 * 60 * 60 * 1000, 5000, () => agora);
    enviados.adicionar('eco');
    agora = 30 * 60 * 1000;
    expect(enviados.contem('eco')).toBe(true);
  });
});

describe('fila de envio', () => {
  it('processa na ordem de chegada, um por vez, com intervalo de 1 a 2,5 s entre envios', async () => {
    let agora = 1000;
    const esperas: number[] = [];
    const ordem: string[] = [];
    let simultaneos = 0;
    let maximo = 0;
    const fila = new FilaDeEnvio({
      aleatorio: () => 1,
      agora: () => agora,
      dormir: async (ms) => {
        esperas.push(ms);
        agora += ms;
      }
    });
    for (const nome of ['a', 'b', 'c']) {
      fila.enfileirar(async () => {
        simultaneos++;
        maximo = Math.max(maximo, simultaneos);
        ordem.push(nome);
        await Promise.resolve();
        simultaneos--;
      });
    }
    await fila.aguardarOciosa();
    expect(ordem).toEqual(['a', 'b', 'c']);
    expect(maximo).toBe(1);
    expect(esperas).toEqual([2500, 2500]);
  });

  it('o intervalo sorteado fica entre 1000 e 2500 ms', async () => {
    let agora = 5000;
    const esperas: number[] = [];
    const fila = new FilaDeEnvio({
      aleatorio: () => 0,
      agora: () => agora,
      dormir: async (ms) => {
        esperas.push(ms);
        agora += ms;
      }
    });
    fila.enfileirar(async () => undefined);
    fila.enfileirar(async () => undefined);
    await fila.aguardarOciosa();
    expect(esperas).toEqual([1000]);
  });

  it('rejeita o envio 51 com a fila cheia', async () => {
    let liberar: () => void = () => undefined;
    const portao = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    const fila = new FilaDeEnvio({ dormir: async () => undefined });
    const resultados: boolean[] = [];
    for (let i = 0; i < 51; i++) {
      resultados.push(fila.enfileirar(() => portao));
    }
    expect(resultados.filter(Boolean)).toHaveLength(50);
    expect(resultados[50]).toBe(false);
    liberar();
    await fila.aguardarOciosa();
  });

  it('uma tarefa que falha nao trava a fila', async () => {
    const fila = new FilaDeEnvio({ dormir: async () => undefined });
    const feitos: string[] = [];
    fila.enfileirar(async () => {
      throw new Error('falha');
    });
    fila.enfileirar(async () => {
      feitos.push('b');
    });
    await fila.aguardarOciosa();
    expect(feitos).toEqual(['b']);
  });
});

describe('persistencia atomica', () => {
  it('grava sem deixar arquivo temporario e guarda a ultima versao boa', async () => {
    const pasta = await mkdtemp(join(tmpdir(), 'gw-arq-'));
    const arquivo = join(pasta, 'meta.json');
    await gravarAtomico(arquivo, '{"v":1}');
    await gravarAtomico(arquivo, '{"v":2}');
    expect(JSON.parse(await readFile(arquivo, 'utf8'))).toEqual({ v: 2 });
    expect(JSON.parse(await readFile(`${arquivo}.bak`, 'utf8'))).toEqual({ v: 1 });
    expect((await readdir(pasta)).filter((nome) => nome.endsWith('.tmp'))).toEqual([]);
  });

  it('desligamento no meio da gravacao: arquivo corrompido volta pela copia boa', async () => {
    const pasta = await mkdtemp(join(tmpdir(), 'gw-arq-'));
    const arquivo = join(pasta, 'creds.json');
    await gravarAtomico(arquivo, '{"registered":true}');
    await gravarAtomico(arquivo, '{"registered":true,"me":"x"}');
    await writeFile(arquivo, '{"registered":tr');
    await writeFile(`${arquivo}.123.tmp`, '{"lixo"');
    expect(await lerComReserva(arquivo)).toEqual({ registered: true });
  });

  it('pareada e so com me e (registered ou account): QR grava account, codigo pedido e nao digitado grava so me', () => {
    expect(credenciaisPareadas({ registered: false, me: { id: '1@s.whatsapp.net' }, account: {} })).toBe(true);
    expect(credenciaisPareadas({ registered: true, me: { id: '1@s.whatsapp.net' } })).toBe(true);
    expect(credenciaisPareadas({ registered: false, me: { id: '1@s.whatsapp.net' } })).toBe(false);
    expect(credenciaisPareadas({ registered: true })).toBe(false);
    expect(credenciaisPareadas({})).toBe(false);
    expect(credenciaisPareadas(null)).toBe(false);
  });

  it('o estado de autenticacao restaura as credenciais gravadas e cai na copia se o arquivo estiver corrompido', async () => {
    const pasta = await mkdtemp(join(tmpdir(), 'gw-auth-'));
    const primeiro = await usarAutenticacaoAtomica(pasta);
    expect(primeiro.registrada()).toBe(false);
    Object.assign(primeiro.state.creds, { registered: false, me: { id: '5511999990000:1@s.whatsapp.net' }, account: { details: 'x' } });
    await primeiro.salvarCredenciais();
    await primeiro.salvarCredenciais();
    await writeFile(join(pasta, 'creds.json'), '{"quebrado');

    const segundo = await usarAutenticacaoAtomica(pasta);
    expect(segundo.registrada()).toBe(true);
    expect(segundo.state.creds.noiseKey).toBeDefined();
  });

  it('chaves de sinal gravam e removem arquivo e copia', async () => {
    const pasta = await mkdtemp(join(tmpdir(), 'gw-keys-'));
    const estado = await usarAutenticacaoAtomica(pasta);
    await estado.state.keys.set({ 'pre-key': { '7': { private: Buffer.from('a'), public: Buffer.from('b') } } } as never);
    const lido = await estado.state.keys.get('pre-key', ['7']);
    expect(Buffer.from(lido['7'].private).toString()).toBe('a');
    await estado.state.keys.set({ 'pre-key': { '7': null } } as never);
    expect((await estado.state.keys.get('pre-key', ['7']))['7']).toBeNull();
    expect(await readdir(pasta)).not.toContain('pre-key-7.json');
  });
});

describe('download de imagem', () => {
  const publico = async () => ['93.184.216.34'];
  const opcoes = (extra = {}) => ({ limiteBytes: 1000, timeoutMs: 1000, hostsPermitidos: [], resolverDns: publico, ...extra });
  const imagem = (tipo = 'image/jpeg', corpo = Buffer.from('conteudo')) =>
    vi.fn(async () => ({ status: 200, contentType: tipo, contentLength: corpo.length, corpo }));

  it('baixa a foto pela URL http(s)', async () => {
    const requisitar = imagem();
    const resultado = await baixarImagem('https://fotos.exemplo.com/a.jpg', opcoes({ requisitar }));
    expect(resultado.mimetype).toBe('image/jpeg');
    expect(resultado.buffer.toString()).toBe('conteudo');
  });

  it('le data URL base64', async () => {
    const resultado = await baixarImagem(`data:image/png;base64,${Buffer.from('png').toString('base64')}`, opcoes());
    expect(resultado.mimetype).toBe('image/png');
  });

  it('recusa destino interno, esquema invalido, credenciais, nao imagem e acima do limite', async () => {
    const requisitar = imagem();
    await expect(baixarImagem('http://127.0.0.1/a.jpg', opcoes({ requisitar }))).rejects.toThrow(ErroDeMidia);
    await expect(baixarImagem('http://metadata.interno/a.jpg', opcoes({ requisitar, resolverDns: async () => ['169.254.169.254'] }))).rejects.toThrow(ErroDeMidia);
    await expect(baixarImagem('http://[::1]/a.jpg', opcoes({ requisitar }))).rejects.toThrow(ErroDeMidia);
    await expect(baixarImagem('ftp://x/a.jpg', opcoes({ requisitar }))).rejects.toThrow(ErroDeMidia);
    await expect(baixarImagem('http://u:p@x.com/a.jpg', opcoes({ requisitar }))).rejects.toThrow(ErroDeMidia);
    await expect(baixarImagem('https://x.com/a', opcoes({ requisitar: imagem('text/html') }))).rejects.toThrow('nao e uma imagem');
    await expect(baixarImagem('https://x.com/a', opcoes({ requisitar: imagem('image/png', Buffer.alloc(2000)) }))).rejects.toThrow('acima do limite');
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('redirect nao e seguido: resposta 3xx vira erro 502 sem nova requisicao', async () => {
    const requisitar = vi.fn(async () => ({ status: 302, contentType: 'text/html', contentLength: 0, corpo: Buffer.alloc(0) }));
    await expect(baixarImagem('https://x.com/a.jpg', opcoes({ requisitar }))).rejects.toMatchObject({ status: 502 });
    expect(requisitar).toHaveBeenCalledTimes(1);
  });

  it('resolve o DNS uma vez e conecta no IP validado', async () => {
    const requisitar = imagem();
    const resolverDns = vi.fn(async () => ['93.184.216.34']);
    await baixarImagem('https://fotos.exemplo.com/a.jpg', opcoes({ requisitar, resolverDns }));
    expect(resolverDns).toHaveBeenCalledTimes(1);
    expect(requisitar.mock.calls[0][1]).toBe('93.184.216.34');
    expect((requisitar.mock.calls[0][0] as URL).hostname).toBe('fotos.exemplo.com');
  });

  it('nao reconecta por nome: se o DNS passar a apontar para IP interno depois da validacao, o IP usado continua o validado', async () => {
    const requisitar = imagem();
    const respostas = [['93.184.216.34'], ['127.0.0.1']];
    const resolverDns = vi.fn(async () => respostas.shift() as string[]);
    await baixarImagem('https://fotos.exemplo.com/a.jpg', opcoes({ requisitar, resolverDns }));
    expect(requisitar.mock.calls[0][1]).toBe('93.184.216.34');
    expect(resolverDns).toHaveBeenCalledTimes(1);
  });

  it('requisitarNoIpFixo conecta no IP informado e mantem o Host original', async () => {
    const http = await import('node:http');
    let hostRecebido = '';
    const servidor = http.createServer((req, res) => {
      hostRecebido = String(req.headers.host);
      res.writeHead(200, { 'content-type': 'image/png' });
      res.end('png');
    });
    await new Promise<void>((ok) => servidor.listen(0, '127.0.0.1', ok));
    const porta = (servidor.address() as { port: number }).port;
    try {
      const resposta = await requisitarNoIpFixo(new URL(`http://nome-que-nao-resolve.invalid:${porta}/a.png`), '127.0.0.1', 2000, 1000);
      expect(resposta.status).toBe(200);
      expect(resposta.corpo.toString()).toBe('png');
      expect(hostRecebido).toBe(`nome-que-nao-resolve.invalid:${porta}`);
    } finally {
      servidor.close();
    }
  });

  it('respeita a lista de hosts permitidos quando configurada', async () => {
    await expect(baixarImagem('https://outro.com/a.jpg', opcoes({ hostsPermitidos: ['fotos.exemplo.com'], requisitar: imagem() }))).rejects.toThrow('nao permitido');
    await expect(baixarImagem('https://fotos.exemplo.com/a.jpg', opcoes({ hostsPermitidos: ['fotos.exemplo.com'], requisitar: imagem() }))).resolves.toBeDefined();
  });

  it('falha de rede e http de erro viram erro 502', async () => {
    const falha = vi.fn(async () => {
      throw new TypeError('falha de rede');
    });
    await expect(baixarImagem('https://x.com/a.jpg', opcoes({ requisitar: falha }))).rejects.toMatchObject({ status: 502 });
    const nao = vi.fn(async () => ({ status: 404, contentType: 'text/plain', contentLength: 1, corpo: Buffer.from('x') }));
    await expect(baixarImagem('https://x.com/a.jpg', opcoes({ requisitar: nao }))).rejects.toMatchObject({ status: 502 });
  });

  it('recusa IPv6 que embute IPv4 interno, com a URL real', async () => {
    const requisitar = imagem();
    for (const host of ['[::ffff:127.0.0.1]', '[::ffff:172.18.0.2]', '[::ffff:169.254.169.254]', '[::7f00:1]', '[64:ff9b::a9fe:a9fe]', '[2002:7f00:1::]', '[2002:a9fe:a9fe::]', '[::ffff:10.0.0.1]', '[::ffff:0:7f00:1]', '[::ffff:0:127.0.0.1]', '[::ffff:0:a9fe:a9fe]', '[64:ff9b:1::7f00:1]', '[2001:0:4136:e378:8000:63bf:3fff:fdd2]', '[100::1]']) {
      await expect(baixarImagem(`http://${host}/a.jpg`, opcoes({ requisitar })), host).rejects.toThrow(ErroDeMidia);
    }
    expect(requisitar).not.toHaveBeenCalled();
    await expect(baixarImagem('http://[2606:4700::1111]/a.jpg', opcoes({ requisitar }))).resolves.toBeDefined();
    await expect(baixarImagem('http://[::ffff:8.8.8.8]/a.jpg', opcoes({ requisitar }))).resolves.toBeDefined();
  });

  it('classifica enderecos privados', () => {
    for (const privado of ['10.0.0.1', '172.16.5.5', '192.168.1.1', '127.0.0.1', '169.254.169.254', '0.0.0.0', '100.64.0.1', '::1', 'fd00::1', 'fe80::1', '::ffff:10.0.0.1', '::ffff:7f00:1', '::ffff:0:7f00:1', '::ffff:0:a9fe:a9fe', '64:ff9b:1::7f00:1', '2001:0:4136:e378:8000:63bf:3fff:fdd2', '100::1', '::ffff:ac12:2', '::ffff:a9fe:a9fe', '::7f00:1', '64:ff9b::a9fe:a9fe', '2002:7f00:1::', 'fec0::1', 'ff02::1', '192.0.0.1', '198.18.0.1', '224.0.0.1', '240.0.0.1', '::']) {
      expect(enderecoPrivado(privado), privado).toBe(true);
    }
    for (const publico of ['93.184.216.34', '8.8.8.8', '172.32.0.1', '2606:4700::1', '::ffff:8.8.8.8', '64:ff9b::808:808', '2002:808:808::']) {
      expect(enderecoPrivado(publico), publico).toBe(false);
    }
  });
});

describe('logs sem dado pessoal', () => {
  function capturar() {
    const linhas: string[] = [];
    const destino = { write: (linha: string) => void linhas.push(linha) };
    return { linhas, logger: criarLogger('debug', destino as never) };
  }

  it('mascara numero e oculta texto, token, qr e segredo', () => {
    const { linhas, logger } = capturar();
    logger.info(
      {
        sessao: 's1',
        numero: mascararNumero('5511999997777@c.us'),
        body: 'segredo do cliente',
        token: 'tok-abc',
        qrcode: 'data:image/png;base64,AAAA',
        erro: new Error('falha com 5511999997777@s.whatsapp.net e 5511999997777')
      },
      'Teste com 5511999997777 no texto'
    );
    const saida = linhas.join('\n');
    expect(saida).toContain('***7777');
    expect(saida).not.toContain('5511999997777');
    expect(saida).not.toContain('segredo do cliente');
    expect(saida).not.toContain('tok-abc');
    expect(saida).not.toContain('AAAA');
  });

  it('sanear remove binarios e limita profundidade', () => {
    expect(sanear(Buffer.from('x'))).toBe('[binario]');
    expect(sanear({ a: { b: { c: { d: { e: 1 } } } } })).toBeDefined();
    expect(sanear('lid 123456789012345@lid')).toContain('lid:***2345');
  });
});

describe('mapa recente', () => {
  it('limita o tamanho descartando o mais antigo e expira por validade', () => {
    let agora = 0;
    const mapa = new MapaRecente<string>(1000, 3, () => agora);
    for (const chave of ['a', 'b', 'c', 'd']) {
      mapa.set(chave, chave.toUpperCase());
    }
    expect(mapa.tamanho).toBe(3);
    expect(mapa.has('a')).toBe(false);
    expect(mapa.get('d')).toBe('D');
    agora = 1001;
    expect(mapa.get('d')).toBeUndefined();
  });
});
