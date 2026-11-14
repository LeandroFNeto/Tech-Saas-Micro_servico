import {
  deleteChat,
  setProfileName,
  showAllContacts,
} from '../controller/deviceController';
import { descreverErroSeguro } from '../util/functions';

const NUMERO = '5511999990000';

function resposta() {
  const res: any = { headersSent: false };
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  res.send = jest.fn().mockReturnValue(res);
  return res;
}

function requisicao(client: any, body: any = {}) {
  return {
    body,
    params: { session: 'sessao' },
    client,
    logger: { error: jest.fn(), warn: jest.fn(), info: jest.fn() },
  } as any;
}

function registrado(req: any, res: any): string {
  const logs = ['error', 'warn', 'info']
    .flatMap((nivel) => req.logger[nivel].mock.calls)
    .map((chamada: any[]) => chamada.map((a) => JSON.stringify(a)).join(' '));
  const respostas = [...res.status.mock.calls, ...res.json.mock.calls].map(
    (chamada: any[]) => JSON.stringify(chamada)
  );
  return [...logs, ...respostas].join('\n');
}

describe('descreverErroSeguro', () => {
  it('mantém tipo, código e status válidos', () => {
    const erro: any = new Error('texto livre');
    erro.name = 'AxiosError';
    erro.code = 'ECONNREFUSED';
    erro.response = { status: 503 };
    expect(descreverErroSeguro(erro)).toBe('AxiosError code ECONNREFUSED HTTP 503');
  });

  it('nome longo vira erro', () => {
    const erro: any = new Error('x');
    erro.name = 'A'.repeat(65);
    expect(descreverErroSeguro(erro)).toBe('erro');
  });

  it('nome com caracteres de controle ou espaço vira erro', () => {
    const erro1: any = { name: 'Erro\nlinha forjada' };
    const erro2: any = { name: 'Erro com espaço' };
    const erro3: any = { name: 'Erro\u0000nulo' };
    expect(descreverErroSeguro(erro1)).toBe('erro');
    expect(descreverErroSeguro(erro2)).toBe('erro');
    expect(descreverErroSeguro(erro3)).toBe('erro');
  });

  it('nome com número de telefone vira erro', () => {
    expect(descreverErroSeguro({ name: NUMERO })).toBe('erro');
    expect(descreverErroSeguro({ name: `Erro_${NUMERO}` })).toBe('erro');
  });

  it('código com número de telefone é omitido', () => {
    expect(descreverErroSeguro({ name: 'Erro', code: NUMERO })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', code: Number(NUMERO) })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', code: `${NUMERO}@c.us` })).toBe('Erro');
  });

  it('código longo ou com caracteres de controle é omitido', () => {
    expect(descreverErroSeguro({ name: 'Erro', code: 'C'.repeat(33) })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', code: 'COD\r\nIGO' })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', code: 'ENOENT' })).toBe('Erro code ENOENT');
    expect(descreverErroSeguro({ name: 'Erro', code: 42 })).toBe('Erro code 42');
  });

  it('status só entra se for inteiro de 100 a 599', () => {
    expect(descreverErroSeguro({ name: 'Erro', response: { status: 99 } })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', response: { status: 600 } })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', response: { status: '404' } })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', response: { status: 4.5 } })).toBe('Erro');
    expect(descreverErroSeguro({ name: 'Erro', response: { status: 404 } })).toBe('Erro HTTP 404');
  });

  it('valor que não é objeto vira erro', () => {
    expect(descreverErroSeguro(undefined)).toBe('erro');
    expect(descreverErroSeguro(null)).toBe('erro');
  });
});

describe('deviceController sem o objeto do erro', () => {
  const objetoDoErro = { erro: true, to: `${NUMERO}@c.us`, body: 'texto secreto' };

  it('rota com error no corpo registra só a descrição segura e responde sem error', async () => {
    const req = requisicao(
      { getAllContacts: jest.fn().mockRejectedValue(objetoDoErro) },
      {}
    );
    const res = resposta();

    await showAllContacts(req, res);

    expect(req.logger.error).toHaveBeenCalledWith('Object');
    expect(res.status).toHaveBeenCalledWith(500);
    const corpo = res.json.mock.calls[0][0];
    expect(corpo.status).toBe('error');
    expect(typeof corpo.message).toBe('string');
    expect(corpo).not.toHaveProperty('error');
    const saida = registrado(req, res);
    expect(saida).not.toContain(NUMERO);
    expect(saida).not.toContain('texto secreto');
  });

  it('setProfileName falha sem vazar o objeto', async () => {
    const req = requisicao(
      { setProfileName: jest.fn().mockRejectedValue(objetoDoErro) },
      { name: 'Novo' }
    );
    const res = resposta();

    await setProfileName(req, res);

    const corpo = res.json.mock.calls[0][0];
    expect(corpo).not.toHaveProperty('error');
    const saida = registrado(req, res);
    expect(saida).not.toContain(NUMERO);
    expect(saida).not.toContain('texto secreto');
  });

  it('returnError responde sem o campo log e registra só a descrição', async () => {
    const erro: any = new Error(`falha para ${NUMERO}`);
    erro.name = 'AxiosError';
    erro.code = NUMERO;
    erro.response = { status: 502 };
    const req = requisicao(
      { deleteChat: jest.fn().mockRejectedValue(erro) },
      { phone: [NUMERO] }
    );
    req.session = 'sessao';
    const res = resposta();

    await deleteChat(req, res);

    expect(res.status).toHaveBeenCalledWith(400);
    const corpo = res.json.mock.calls[0][0];
    expect(corpo.status).toBe('Error');
    expect(corpo.response.message).toBeDefined();
    expect(corpo.response.session).toBeDefined();
    expect(corpo.response).not.toHaveProperty('log');
    expect(req.logger.error).toHaveBeenCalledWith('AxiosError HTTP 502');
    expect(registrado(req, res)).not.toContain(NUMERO);
  });
});
