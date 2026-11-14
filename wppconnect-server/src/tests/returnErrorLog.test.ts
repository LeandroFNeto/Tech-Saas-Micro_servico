import { sendFile, sendMentioned, sendMessage } from '../controller/messageController';

const NUMERO = '5511999990000@c.us';
const TEXTO = 'texto secreto';

function resposta() {
  const res: any = { headersSent: false };
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  res.send = jest.fn().mockReturnValue(res);
  return res;
}

function requisicao(client: any) {
  return {
    body: { phone: ['5511999990000'], message: TEXTO, path: 'arquivo.png' },
    client,
    logger: { error: jest.fn(), warn: jest.fn(), info: jest.fn() },
    io: { emit: jest.fn() },
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

describe('returnError sem número nem texto', () => {
  it('sendText que rejeita não vaza no logger nem na resposta', async () => {
    const req = requisicao({
      sendText: jest
        .fn()
        .mockRejectedValue({ erro: true, to: NUMERO, body: TEXTO }),
    });
    const res = resposta();

    await sendMessage(req, res);

    expect(res.status).toHaveBeenCalledWith(500);
    const saida = registrado(req, res);
    expect(saida).not.toContain('5511999990000');
    expect(saida).not.toContain(TEXTO);
  });

  it('sendImage que rejeita não vaza no logger nem na resposta', async () => {
    const req = requisicao({
      sendFile: jest
        .fn()
        .mockRejectedValue({ erro: true, to: NUMERO, text: TEXTO }),
    });
    const res = resposta();

    await sendFile(req, res);

    expect(res.status).toHaveBeenCalledWith(500);
    const saida = registrado(req, res);
    expect(saida).not.toContain('5511999990000');
    expect(saida).not.toContain(TEXTO);
  });

  it('sendMentioned que rejeita não vaza no logger nem na resposta', async () => {
    const req = requisicao({
      sendMentioned: jest
        .fn()
        .mockRejectedValue({ erro: true, to: NUMERO, body: TEXTO }),
    });
    req.body.mentioned = ['5511999990000'];
    const res = resposta();

    await sendMentioned(req, res);

    expect(res.status).toHaveBeenCalledWith(500);
    const saida = registrado(req, res);
    expect(saida).not.toContain('5511999990000');
    expect(saida).not.toContain(TEXTO);
  });
});
