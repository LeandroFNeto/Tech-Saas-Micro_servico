import { describe, expect, it } from 'vitest';
import { converterMensagem, type MensagemBaileys } from '../src/mensagem.js';

interface Payload {
  event?: string;
  session?: string;
  type?: string;
  fromMe?: boolean;
  timestamp?: number;
  to?: string;
  isGroupMsg?: boolean;
  from?: string;
  body?: string;
  sender?: { id?: string; pushname?: string } | string;
  notifyName?: string;
  qrcode?: string;
}

const tem = (valor: string | undefined | null): valor is string => valor !== undefined && valor !== null && valor.trim() !== '';

function dto(payload: Payload) {
  const senderId = typeof payload.sender === 'string' ? payload.sender : payload.sender?.id;
  const resolverLid = (jid: string): string =>
    jid.includes('@lid') && tem(senderId) && (senderId.includes('@c.us') || senderId.includes('@s.whatsapp.net')) ? senderId : jid;
  const resolverRemoteJid = (): string | null => {
    if (tem(payload.from)) {
      return resolverLid(payload.from);
    }
    if (tem(senderId)) {
      return resolverLid(senderId);
    }
    return null;
  };
  const enviadaPorMim = (): boolean => payload.event === 'onselfmessage' || payload.fromMe === true;
  const digitos = (jid: string): string => jid.split('@')[0].replace(/\D/g, '');
  const chaveEstado = (jid: string): string => {
    const numeros = digitos(jid);
    return !jid.includes('@lid') && numeros.startsWith('55') && numeros.length === 13 ? numeros.slice(0, 4) + numeros.slice(5) : numeros;
  };
  return {
    ehEventoQrcode: () => tem(payload.qrcode) || payload.event === 'qrcode',
    ehEventoConexao: () => (payload.event ?? '').toLowerCase().includes('status-find'),
    ehMensagemAntiga: (agoraSegundos: number) => payload.timestamp !== undefined && agoraSegundos - payload.timestamp > 120,
    enviadaPorMim,
    resolverRemoteJid,
    resolverContatoAlvo: () => (enviadaPorMim() && tem(payload.to) ? payload.to : resolverRemoteJid()),
    resolverJidBruto: () => payload.from ?? senderId ?? null,
    ehGrupo: () => payload.isGroupMsg === true || (payload.from ?? '').includes('@g.us'),
    ehMensagemDeTexto: () => (tem(payload.type) ? payload.type.trim().toLowerCase() === 'chat' : tem(payload.body)),
    chaveEstado,
    nome: () => (tem(payload.notifyName) ? payload.notifyName.trim() : typeof payload.sender === 'object' && tem(payload.sender.pushname) ? payload.sender.pushname.trim() : null)
  };
}

const contexto = {
  sessao: 'sessao_recanto_01',
  numeroConectado: '5511900000000@c.us',
  aoMensagemDoDono: true,
  ehEcoDoBot: () => false
};

const agora = 1730592030;

function postar(msg: MensagemBaileys): Payload {
  const resultado = converterMensagem(msg, contexto);
  if (!resultado.ok) {
    throw new Error(resultado.motivo);
  }
  return JSON.parse(JSON.stringify(resultado.payload)) as Payload;
}

describe('payload do gateway lido como o WhatsappWebhookDTO do Java', () => {
  it('cliente: passa por todos os filtros do ControllerWhatsapp e chega a strategy com o nome do contato', () => {
    const payload = postar({
      key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: false, id: 'C1' },
      message: { conversation: 'Olá, gostaria de reservar' },
      messageTimestamp: { low: 1730592000, high: 0, unsigned: true },
      pushName: 'Joao Silva'
    });
    const d = dto(payload);
    expect(d.ehEventoQrcode()).toBe(false);
    expect(d.ehEventoConexao()).toBe(false);
    expect(d.ehMensagemAntiga(agora)).toBe(false);
    expect(d.enviadaPorMim()).toBe(false);
    expect(d.ehGrupo()).toBe(false);
    expect(d.ehMensagemDeTexto()).toBe(true);
    expect(d.resolverRemoteJid()).toBe('5511999999999@c.us');
    expect(d.nome()).toBe('Joao Silva');
  });

  it('timestamp antigo e descartado pelo filtro de 120 s do Java', () => {
    const payload = postar({
      key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: false, id: 'C2' },
      message: { conversation: 'oi' },
      messageTimestamp: 1730591000
    });
    expect(dto(payload).ehMensagemAntiga(agora)).toBe(true);
  });

  it('dono (texto): Anti-X9 liga a trava na chave do cliente, sem o :dispositivo', () => {
    const payload = postar({
      key: { remoteJid: '5511999990000:12@s.whatsapp.net', fromMe: true, id: 'D1' },
      message: { conversation: 'Teste do dono' },
      messageTimestamp: 1730592000,
      pushName: 'Nome Do Dono'
    });
    const d = dto(payload);
    expect(d.enviadaPorMim()).toBe(true);
    expect(d.resolverContatoAlvo()).toBe('5511999990000@c.us');
    expect(d.chaveEstado(d.resolverContatoAlvo() as string)).toBe('551199990000');
    expect(d.nome()).toBeNull();
  });

  it('dono (foto): tambem chega como mensagem do dono, antes do filtro de tipo', () => {
    const payload = postar({
      key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: true, id: 'D2' },
      message: { imageMessage: { caption: 'foto' } },
      messageTimestamp: 1730592000
    });
    const d = dto(payload);
    expect(payload.event).toBe('onselfmessage');
    expect(d.enviadaPorMim()).toBe(true);
    expect(d.ehMensagemDeTexto()).toBe(false);
  });

  it('@lid com numero: o Java resolve o numero como remoteJid e registra o apelido pelo from', () => {
    const payload = postar({
      key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L1', senderPn: '5511999990000@s.whatsapp.net' },
      message: { conversation: 'oi' },
      messageTimestamp: 1730592000
    });
    const d = dto(payload);
    expect(payload.from).toBe('123456789012345@lid');
    expect(d.resolverRemoteJid()).toBe('5511999990000@c.us');
    expect(d.resolverJidBruto()).toBe('123456789012345@lid');
    expect(d.resolverJidBruto()).not.toBe(d.resolverRemoteJid());
  });

  it('@lid sem numero: o Java atende pelo proprio @lid', () => {
    const payload = postar({
      key: { remoteJid: '123456789012345@lid', fromMe: false, id: 'L2' },
      message: { conversation: 'oi' },
      messageTimestamp: 1730592000
    });
    expect(dto(payload).resolverRemoteJid()).toBe('123456789012345@lid');
  });

  it('o payload so tem campos que o DTO conhece', () => {
    const conhecidos = new Set(['event', 'session', 'id', 'fromMe', 'timestamp', 'from', 'to', 'type', 'body', 'isGroupMsg', 'sender', 'notifyName']);
    const payload = postar({
      key: { remoteJid: '5511999999999@s.whatsapp.net', fromMe: false, id: 'C9' },
      message: { conversation: 'oi' },
      messageTimestamp: 1730592000,
      pushName: 'Ana'
    });
    expect(Object.keys(payload).filter((campo) => !conhecidos.has(campo))).toEqual([]);
    expect(typeof payload.timestamp).toBe('number');
    expect(Number.isInteger(payload.timestamp)).toBe(true);
  });
});
