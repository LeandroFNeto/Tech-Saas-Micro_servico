import { describe, expect, it } from 'vitest';
import { converterMensagem, converterTimestamp, type ContextoConversao, type MensagemBaileys } from '../src/mensagem.js';
import { montarDestino, normalizar } from '../src/jid.js';

const contexto = (extra: Partial<ContextoConversao> = {}): ContextoConversao => ({
  sessao: 'sessao_recanto_01',
  numeroConectado: '5511900000000@c.us',
  lidConectado: '88880000@lid',
  aoMensagemDoDono: true,
  ehEcoDoBot: () => false,
  ...extra
});

const cliente = (extra: Partial<MensagemBaileys> = {}): MensagemBaileys => ({
  key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: false, id: 'AAA1' },
  message: { conversation: 'Olá, gostaria de reservar' },
  messageTimestamp: 1730592000,
  pushName: 'Joao Silva',
  ...extra
});

const dono = (mensagem: Record<string, any>, chat = '5511999999999@s.whatsapp.net'): MensagemBaileys => ({
  key: { remoteJid: chat, fromMe: true, id: 'DONO1' },
  message: mensagem,
  messageTimestamp: 1730592000,
  pushName: 'Nome Do Dono'
});

function payloadDe(msg: MensagemBaileys, ctx = contexto()) {
  const resultado = converterMensagem(msg, ctx);
  if (!resultado.ok) {
    throw new Error(`ignorada: ${resultado.motivo}`);
  }
  return resultado.payload;
}

function motivoDe(msg: MensagemBaileys, ctx = contexto()) {
  const resultado = converterMensagem(msg, ctx);
  return resultado.ok ? 'POSTADA' : resultado.motivo;
}

describe('mensagem do cliente', () => {
  it('texto simples vira onmessage no formato do WhatsappWebhookDTO', () => {
    expect(payloadDe(cliente())).toEqual({
      event: 'onmessage',
      session: 'sessao_recanto_01',
      id: 'false_5511999999999@c.us_AAA1',
      fromMe: false,
      timestamp: 1730592000,
      from: '5511999999999@c.us',
      to: '5511900000000@c.us',
      type: 'chat',
      body: 'Olá, gostaria de reservar',
      isGroupMsg: false,
      sender: { id: '5511999999999@c.us', pushname: 'Joao Silva' },
      notifyName: 'Joao Silva'
    });
  });

  it('pushName vazio vai ausente', () => {
    const payload = payloadDe(cliente({ pushName: '  ' }));
    expect(payload.notifyName).toBeUndefined();
    expect(payload.sender.pushname).toBeUndefined();
  });

  it('JID com sufixo de dispositivo vira numero puro em from, to e sender.id', () => {
    const payload = payloadDe(
      cliente({ key: { remoteJid: '5511999990000:12@s.whatsapp.net', fromMe: false, id: 'B1' } }),
      contexto({ numeroConectado: normalizar('5511900000000:12@s.whatsapp.net') })
    );
    expect(payload.from).toBe('5511999990000@c.us');
    expect(payload.sender.id).toBe('5511999990000@c.us');
    expect(payload.to).toBe('5511900000000@c.us');
    expect(JSON.stringify(payload)).not.toContain(':12');
  });

  it('timestamp em Long vira inteiro em segundos', () => {
    expect(payloadDe(cliente({ messageTimestamp: { low: 1730592000, high: 0, unsigned: true } })).timestamp).toBe(1730592000);
    expect(payloadDe(cliente({ messageTimestamp: { toNumber: () => 1730592005 } })).timestamp).toBe(1730592005);
    expect(payloadDe(cliente({ messageTimestamp: '1730592001' })).timestamp).toBe(1730592001);
    expect(converterTimestamp(1730592000123)).toBe(1730592000);
  });

  it('texto em resposta, mensagem temporaria e visualizacao unica e desembrulhado como chat', () => {
    const resposta = cliente({ message: { extendedTextMessage: { text: 'Quero sábado', contextInfo: {} } } });
    expect(payloadDe(resposta)).toMatchObject({ type: 'chat', body: 'Quero sábado' });

    const efemera = cliente({ message: { ephemeralMessage: { message: { extendedTextMessage: { text: 'Oi efêmero' } } } } });
    expect(payloadDe(efemera)).toMatchObject({ type: 'chat', body: 'Oi efêmero' });

    const unica = cliente({ message: { viewOnceMessageV2: { message: { conversation: 'Oi única' } } } });
    expect(payloadDe(unica)).toMatchObject({ type: 'chat', body: 'Oi única' });

    const aninhada = cliente({
      message: { ephemeralMessage: { message: { viewOnceMessage: { message: { conversation: 'Fundo' } } } } }
    });
    expect(payloadDe(aninhada)).toMatchObject({ type: 'chat', body: 'Fundo' });
  });

  it('midia vira o tipo da midia com a legenda no body', () => {
    expect(payloadDe(cliente({ message: { imageMessage: { caption: 'foto do salão' } } }))).toMatchObject({ type: 'image', body: 'foto do salão' });
    expect(payloadDe(cliente({ message: { videoMessage: {} } }))).toMatchObject({ type: 'video', body: '' });
    expect(payloadDe(cliente({ message: { audioMessage: { ptt: false } } }))).toMatchObject({ type: 'audio' });
    expect(payloadDe(cliente({ message: { audioMessage: { ptt: true } } }))).toMatchObject({ type: 'ptt' });
    expect(payloadDe(cliente({ message: { documentMessage: { caption: 'contrato' } } }))).toMatchObject({ type: 'document', body: 'contrato' });
    expect(payloadDe(cliente({ message: { documentWithCaptionMessage: { message: { documentMessage: { caption: 'doc' } } } } }))).toMatchObject({ type: 'document', body: 'doc' });
    expect(payloadDe(cliente({ message: { stickerMessage: {} } }))).toMatchObject({ type: 'sticker' });
    expect(payloadDe(cliente({ message: { locationMessage: {} } }))).toMatchObject({ type: 'location' });
    expect(payloadDe(cliente({ message: { contactMessage: {} } }))).toMatchObject({ type: 'vcard' });
  });

  it('reacao, edicao, apagar, enquete, chamada e sincronizacao do cliente nao viram evento', () => {
    const casos: Array<Record<string, any>> = [
      { reactionMessage: { text: '👍' } },
      { protocolMessage: { type: 0 } },
      { protocolMessage: { type: 14, editedMessage: { conversation: 'novo' } } },
      { editedMessage: { message: { protocolMessage: { type: 14 } } } },
      { pollCreationMessage: { name: 'Enquete' } },
      { pollUpdateMessage: {} },
      { call: {} },
      { senderKeyDistributionMessage: {} },
      { protocolMessage: { type: 5, historySyncNotification: {} } }
    ];
    for (const message of casos) {
      expect(motivoDe(cliente({ message }))).toBe('tipo_nao_suportado');
    }
    expect(motivoDe(cliente({ message: null }))).toBe('tipo_nao_suportado');
    expect(motivoDe(cliente({ message: { conversation: '   ' } }))).toBe('sem_conteudo');
  });

  it('chat @lid sem numero conhecido mantem o @lid em from e sender.id', () => {
    const payload = payloadDe(cliente({ key: { remoteJid: '123456789012345:7@lid', fromMe: false, id: 'L1' } }));
    expect(payload.from).toBe('123456789012345@lid');
    expect(payload.sender.id).toBe('123456789012345@lid');
  });

  it('chat @lid com numero no evento: from fica @lid e sender.id o numero @c.us', () => {
    const porSenderPn = payloadDe(cliente({ key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L2', senderPn: '5511999990000@s.whatsapp.net' } }));
    expect(porSenderPn.from).toBe('123456789012345@lid');
    expect(porSenderPn.sender.id).toBe('5511999990000@c.us');

    const porAlt = payloadDe(cliente({ key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L3', remoteJidAlt: '5511999990000:3@s.whatsapp.net' } }));
    expect(porAlt.sender.id).toBe('5511999990000@c.us');
  });

  it('chat @lid com numero so no mapa do Baileys usa o mapa', () => {
    const payload = payloadDe(
      cliente({ key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L4' } }),
      contexto({ resolverNumeroDoLid: (lid) => (lid === '123456789012345@lid' ? '5511999990000@s.whatsapp.net' : undefined) })
    );
    expect(payload.from).toBe('123456789012345@lid');
    expect(payload.sender.id).toBe('5511999990000@c.us');
  });
});

describe('conversas que nao geram evento', () => {
  it('grupo, status, lista de transmissao, canal, comunidade e Meta AI sao ignorados, do cliente e do dono', () => {
    const chats = ['120363000000000@g.us', 'status@broadcast', '5511999999999@broadcast', '120363111@newsletter', '120363222@g.us', '13135550002@bot'];
    for (const chat of chats) {
      expect(motivoDe(cliente({ key: { remoteJid: chat, fromMe: false, id: 'X1' } }))).toBe('conversa_nao_suportada');
      expect(motivoDe(dono({ conversation: 'oi' }, chat))).toBe('conversa_nao_suportada');
    }
  });

  it('conversa com o proprio numero e ignorada', () => {
    expect(motivoDe(dono({ conversation: 'lembrete' }, '5511900000000@s.whatsapp.net'))).toBe('conversa_com_o_proprio_numero');
    expect(motivoDe(dono({ conversation: 'lembrete' }, '88880000@lid'))).toBe('conversa_com_o_proprio_numero');
  });

  it('mensagem sem id ou sem chat e ignorada', () => {
    expect(motivoDe(cliente({ key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: false } }))).toBe('sem_id');
    expect(motivoDe(cliente({ key: { fromMe: false, id: 'Z' } }))).toBe('conversa_nao_suportada');
  });
});

describe('mensagem do dono (onselfmessage)', () => {
  it('texto do dono liga a trava: from e o numero conectado e to e o cliente', () => {
    const payload = payloadDe(dono({ conversation: 'Teste do dono' }));
    expect(payload).toMatchObject({
      event: 'onselfmessage',
      fromMe: true,
      from: '5511900000000@c.us',
      to: '5511999999999@c.us',
      type: 'chat',
      body: 'Teste do dono',
      id: 'true_5511999999999@c.us_DONO1'
    });
  });

  it('foto, audio e figurinha do dono tambem geram onselfmessage com o tipo da midia', () => {
    expect(payloadDe(dono({ imageMessage: { caption: 'foto' } }))).toMatchObject({ event: 'onselfmessage', type: 'image', body: 'foto' });
    expect(payloadDe(dono({ audioMessage: { ptt: true } }))).toMatchObject({ event: 'onselfmessage', type: 'ptt' });
    expect(payloadDe(dono({ stickerMessage: {} }))).toMatchObject({ event: 'onselfmessage', type: 'sticker' });
  });

  it('reacao, edicao e apagar do dono nao ligam a trava', () => {
    expect(motivoDe(dono({ reactionMessage: { text: '❤️' } }))).toBe('tipo_nao_suportado');
    expect(motivoDe(dono({ protocolMessage: { type: 14, editedMessage: { conversation: 'x' } } }))).toBe('tipo_nao_suportado');
    expect(motivoDe(dono({ protocolMessage: { type: 0 } }))).toBe('tipo_nao_suportado');
    expect(motivoDe(dono({ pollUpdateMessage: {} }))).toBe('tipo_nao_suportado');
  });

  it('pushName do dono nunca vai no payload', () => {
    const payload = payloadDe(dono({ conversation: 'oi' }));
    const texto = JSON.stringify(payload);
    expect(texto).not.toContain('Nome Do Dono');
    expect(payload.notifyName).toBeUndefined();
    expect(payload.sender.pushname).toBeUndefined();
    expect('pushname' in payload).toBe(false);
  });

  it('eco do envio do bot e descartado pelo id enviado pelo gateway', () => {
    const ctx = contexto({ ehEcoDoBot: (id) => id === 'DONO1' });
    expect(motivoDe(dono({ conversation: 'Olá! Como posso ajudar?' }), ctx)).toBe('eco_do_bot');
    expect(motivoDe(dono({ imageMessage: { caption: 'legenda' } }), ctx)).toBe('eco_do_bot');
  });

  it('WPPCONNECT_ON_SELF_MESSAGE=false desliga o onselfmessage', () => {
    expect(motivoDe(dono({ conversation: 'oi' }), contexto({ aoMensagemDoDono: false }))).toBe('mensagem_do_dono_desligada');
  });

  it('dono em chat @lid com numero conhecido usa o numero em to', () => {
    const msg = dono({ conversation: 'oi' }, '123456789012345@lid');
    msg.key.remoteJidAlt = '5511999990000@s.whatsapp.net';
    const payload = payloadDe(msg);
    expect(payload.to).toBe('5511999990000@c.us');
    expect(payload.id).toBe('true_123456789012345@lid_DONO1');
  });

  it('dono em chat @lid sem numero usa o @lid em to', () => {
    expect(payloadDe(dono({ conversation: 'oi' }, '123456789012345@lid')).to).toBe('123456789012345@lid');
  });
});

describe('destino do envio', () => {
  it('isLid, sufixo @lid ou 14 digitos ou mais vao para @lid sem conferir o numero', () => {
    expect(montarDestino('123456789012345', true)).toEqual({ tipo: 'lid', jid: '123456789012345@lid' });
    expect(montarDestino('123456789012345@lid', false)).toEqual({ tipo: 'lid', jid: '123456789012345@lid' });
    expect(montarDestino('12345678901234', false)).toEqual({ tipo: 'lid', jid: '12345678901234@lid' });
  });

  it('numero comum fica em digitos para a conferencia no WhatsApp', () => {
    expect(montarDestino('551188887777', false)).toEqual({ tipo: 'numero', digitos: '551188887777' });
    expect(montarDestino('5511988887777', false)).toEqual({ tipo: 'numero', digitos: '5511988887777' });
  });

  it('numero invalido retorna nulo', () => {
    expect(montarDestino('abc', false)).toBeNull();
    expect(montarDestino('123', false)).toBeNull();
    expect(montarDestino('', false)).toBeNull();
  });
});
