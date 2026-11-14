import statusConnection from '../middleware/statusConnection';

const NUMERO = '5511987654321';

function resposta() {
  const res: any = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  return res;
}

function saidas(espioes: jest.SpyInstance[]): string {
  return espioes
    .flatMap((espiao) => espiao.mock.calls)
    .map((chamada) => chamada.map((a: any) => JSON.stringify(a)).join(' '))
    .join('\n');
}

describe('statusConnection sem número no log', () => {
  let espioes: jest.SpyInstance[];

  beforeEach(() => {
    espioes = [
      jest.spyOn(console, 'log').mockImplementation(() => undefined),
      jest.spyOn(console, 'error').mockImplementation(() => undefined),
      jest.spyOn(console, 'warn').mockImplementation(() => undefined),
    ];
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('número existente não aparece em nenhuma saída', async () => {
    const req: any = {
      body: { phone: [NUMERO] },
      client: {
        isConnected: jest.fn().mockResolvedValue(true),
        checkNumberStatus: jest.fn().mockResolvedValue({
          numberExists: true,
          id: { _serialized: `${NUMERO}@c.us` },
        }),
      },
    };
    const next = jest.fn();

    await statusConnection(req, resposta(), next);

    expect(next).toHaveBeenCalled();
    expect(saidas(espioes)).not.toContain(NUMERO);
  });

  it('consulta que rejeita não registra o número nem o objeto do erro', async () => {
    const req: any = {
      body: { phone: [NUMERO] },
      client: {
        isConnected: jest.fn().mockResolvedValue(true),
        checkNumberStatus: jest
          .fn()
          .mockRejectedValue(new Error(`falha para ${NUMERO}@c.us`)),
      },
    };
    const res = resposta();

    await statusConnection(req, res, jest.fn());

    expect(res.status).toHaveBeenCalledWith(400);
    expect(saidas(espioes)).not.toContain(NUMERO);
  });
});
