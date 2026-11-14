import { conversaAceita, ehLid, normalizar, usuarioDe } from './jid.js';

export interface ChaveBaileys {
  remoteJid?: string | null;
  fromMe?: boolean | null;
  id?: string | null;
  participant?: string | null;
  senderPn?: string | null;
  participantPn?: string | null;
  remoteJidAlt?: string | null;
  participantAlt?: string | null;
}

export interface MensagemBaileys {
  key: ChaveBaileys;
  message?: Record<string, any> | null;
  messageTimestamp?: unknown;
  pushName?: string | null;
  messageStubType?: number | null;
}

export interface ContextoConversao {
  sessao: string;
  numeroConectado: string | null;
  lidConectado?: string | null;
  aoMensagemDoDono: boolean;
  ehEcoDoBot: (idMensagem: string) => boolean;
  resolverNumeroDoLid?: (lid: string) => string | null | undefined;
}

export interface PayloadWebhook {
  event: 'onmessage' | 'onselfmessage';
  session: string;
  id: string;
  fromMe: boolean;
  timestamp: number;
  from: string;
  to: string;
  type: string;
  body: string;
  isGroupMsg: false;
  sender: { id: string; pushname?: string };
  notifyName?: string;
}

export type ResultadoConversao =
  | { ok: true; payload: PayloadWebhook; idBaileys: string }
  | { ok: false; motivo: string };

const ENVOLTORIOS = [
  'ephemeralMessage',
  'viewOnceMessage',
  'viewOnceMessageV2',
  'viewOnceMessageV2Extension',
  'documentWithCaptionMessage'
];

const IGNORADOS = [
  'reactionMessage',
  'encReactionMessage',
  'protocolMessage',
  'editedMessage',
  'pollCreationMessage',
  'pollCreationMessageV2',
  'pollCreationMessageV3',
  'pollUpdateMessage',
  'call',
  'scheduledCallCreationMessage',
  'keepInChatMessage',
  'pinInChatMessage'
];

export function desembrulhar(mensagem: Record<string, any> | null | undefined): Record<string, any> | null {
  let atual: Record<string, any> | null | undefined = mensagem;
  for (let nivel = 0; nivel < 6 && atual; nivel++) {
    const chave = ENVOLTORIOS.find((nome) => atual![nome]?.message);
    if (!chave) {
      return atual;
    }
    atual = atual[chave].message;
  }
  return atual ?? null;
}

export function converterTimestamp(valor: unknown): number {
  let numero = 0;
  if (typeof valor === 'number') {
    numero = valor;
  } else if (typeof valor === 'string') {
    numero = Number(valor);
  } else if (valor && typeof valor === 'object') {
    const longo = valor as { toNumber?: () => number; low?: number; high?: number; unsigned?: boolean };
    if (typeof longo.toNumber === 'function') {
      numero = longo.toNumber();
    } else if (typeof longo.low === 'number') {
      const alto = typeof longo.high === 'number' ? longo.high : 0;
      numero = (alto >>> 0) * 4294967296 + (longo.low >>> 0);
    }
  }
  if (!Number.isFinite(numero) || numero <= 0) {
    return Math.floor(Date.now() / 1000);
  }
  if (numero > 100_000_000_000) {
    numero = numero / 1000;
  }
  return Math.floor(numero);
}

interface Conteudo {
  tipo: string;
  corpo: string;
}

export function extrairConteudo(mensagem: Record<string, any> | null): Conteudo | null {
  if (!mensagem) {
    return null;
  }
  if (IGNORADOS.some((nome) => mensagem[nome])) {
    return null;
  }
  if (typeof mensagem.conversation === 'string') {
    return { tipo: 'chat', corpo: mensagem.conversation };
  }
  if (mensagem.extendedTextMessage) {
    return { tipo: 'chat', corpo: String(mensagem.extendedTextMessage.text ?? '') };
  }
  if (mensagem.imageMessage) {
    return { tipo: 'image', corpo: String(mensagem.imageMessage.caption ?? '') };
  }
  if (mensagem.videoMessage) {
    return { tipo: 'video', corpo: String(mensagem.videoMessage.caption ?? '') };
  }
  if (mensagem.audioMessage) {
    return { tipo: mensagem.audioMessage.ptt ? 'ptt' : 'audio', corpo: '' };
  }
  if (mensagem.documentMessage) {
    return { tipo: 'document', corpo: String(mensagem.documentMessage.caption ?? '') };
  }
  if (mensagem.stickerMessage) {
    return { tipo: 'sticker', corpo: '' };
  }
  if (mensagem.locationMessage || mensagem.liveLocationMessage) {
    return { tipo: 'location', corpo: '' };
  }
  if (mensagem.contactMessage || mensagem.contactsArrayMessage) {
    return { tipo: 'vcard', corpo: '' };
  }
  return null;
}

function numeroDoChat(
  chat: string,
  chave: ChaveBaileys,
  resolver?: (lid: string) => string | null | undefined
): string | null {
  if (!ehLid(chat)) {
    return normalizar(chat);
  }
  const candidato = chave.remoteJidAlt ?? (chave.fromMe ? null : chave.senderPn) ?? null;
  const normalizadoDoEvento = candidato && !ehLid(candidato) ? normalizar(candidato) : null;
  if (normalizadoDoEvento) {
    return normalizadoDoEvento;
  }
  const usuario = usuarioDe(chat);
  if (usuario && resolver) {
    const doMapa = resolver(`${usuario}@lid`);
    if (doMapa) {
      const normalizado = normalizar(doMapa);
      if (normalizado && !ehLid(normalizado)) {
        return normalizado;
      }
    }
  }
  return null;
}

export function converterMensagem(msg: MensagemBaileys, contexto: ContextoConversao): ResultadoConversao {
  const chave = msg.key ?? {};
  const chat = chave.remoteJid ?? null;

  if (!chat || !conversaAceita(chat)) {
    return { ok: false, motivo: 'conversa_nao_suportada' };
  }
  if (!chave.id) {
    return { ok: false, motivo: 'sem_id' };
  }
  const conteudo = extrairConteudo(desembrulhar(msg.message));
  if (!conteudo) {
    return { ok: false, motivo: 'tipo_nao_suportado' };
  }
  if (conteudo.tipo === 'chat' && conteudo.corpo.trim() === '') {
    return { ok: false, motivo: 'sem_conteudo' };
  }

  const fromMe = chave.fromMe === true;
  const timestamp = converterTimestamp(msg.messageTimestamp);
  const chatNormalizado = normalizar(chat) as string;
  const numeroCliente = numeroDoChat(chat, chave, contexto.resolverNumeroDoLid);
  const donoNormalizado = contexto.numeroConectado ? normalizar(contexto.numeroConectado) : null;

  if (fromMe) {
    if (!contexto.aoMensagemDoDono) {
      return { ok: false, motivo: 'mensagem_do_dono_desligada' };
    }
    if (contexto.ehEcoDoBot(chave.id)) {
      return { ok: false, motivo: 'eco_do_bot' };
    }
    const usuarioChat = usuarioDe(chat);
    const usuarioDono = usuarioDe(donoNormalizado);
    const usuarioLidDono = usuarioDe(contexto.lidConectado ?? null);
    if (usuarioChat !== null && (usuarioChat === usuarioDono || usuarioChat === usuarioLidDono)) {
      return { ok: false, motivo: 'conversa_com_o_proprio_numero' };
    }
    const destino = numeroCliente ?? chatNormalizado;
    return {
      ok: true,
      idBaileys: chave.id,
      payload: {
        event: 'onselfmessage',
        session: contexto.sessao,
        id: `true_${chatNormalizado}_${chave.id}`,
        fromMe: true,
        timestamp,
        from: donoNormalizado ?? destino,
        to: destino,
        type: conteudo.tipo,
        body: conteudo.corpo,
        isGroupMsg: false,
        sender: { id: donoNormalizado ?? destino }
      }
    };
  }

  const nome = (msg.pushName ?? '').trim();
  const remetente = numeroCliente ?? chatNormalizado;
  const sender: { id: string; pushname?: string } = { id: remetente };
  if (nome !== '') {
    sender.pushname = nome;
  }
  const payload: PayloadWebhook = {
    event: 'onmessage',
    session: contexto.sessao,
    id: `false_${chatNormalizado}_${chave.id}`,
    fromMe: false,
    timestamp,
    from: chatNormalizado,
    to: donoNormalizado ?? '',
    type: conteudo.tipo,
    body: conteudo.corpo,
    isGroupMsg: false,
    sender
  };
  if (nome !== '') {
    payload.notifyName = nome;
  }
  return { ok: true, idBaileys: chave.id, payload };
}
