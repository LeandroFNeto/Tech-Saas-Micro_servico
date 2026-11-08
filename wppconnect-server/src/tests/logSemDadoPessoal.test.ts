import { descreverFalhaDoWebhook } from '../util/functions';

describe('log do gateway sem dado pessoal', () => {
  afterEach(() => {
    jest.resetModules();
  });

  it('registra a partir do nível info', () => {
    const config = require('../config').default;
    expect(config.log.level).toBe('info');
  });

  it('falha do webhook traz só o código HTTP, sem o corpo da mensagem', () => {
    const erro = {
      response: {
        status: 503,
        data: { texto: 'mensagem do cliente', numero: '5511999990000' },
      },
      config: { data: '{"body":"mensagem do cliente"}' },
    };
    const texto = descreverFalhaDoWebhook(erro);
    expect(texto).toContain('503');
    expect(texto).not.toContain('mensagem do cliente');
    expect(texto).not.toContain('5511999990000');
  });

  it('falha de rede traz só o tipo da falha', () => {
    const texto = descreverFalhaDoWebhook({ code: 'ECONNREFUSED' });
    expect(texto).toContain('ECONNREFUSED');
  });
});
