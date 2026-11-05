describe('config.webhook.onSelfMessage', () => {
  const original = process.env.WPPCONNECT_ON_SELF_MESSAGE;

  afterEach(() => {
    if (original === undefined) {
      delete process.env.WPPCONNECT_ON_SELF_MESSAGE;
    } else {
      process.env.WPPCONNECT_ON_SELF_MESSAGE = original;
    }
    jest.resetModules();
  });

  it('vem ligado quando a variável não existe', () => {
    delete process.env.WPPCONNECT_ON_SELF_MESSAGE;
    jest.resetModules();
    const config = require('../config').default;
    expect(config.webhook.onSelfMessage).toBe(true);
  });

  it('desliga quando a variável vale false', () => {
    process.env.WPPCONNECT_ON_SELF_MESSAGE = 'false';
    jest.resetModules();
    const config = require('../config').default;
    expect(config.webhook.onSelfMessage).toBe(false);
  });
});
