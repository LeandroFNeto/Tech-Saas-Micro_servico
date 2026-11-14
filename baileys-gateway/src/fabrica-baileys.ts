import makeWASocket, { Browsers, fetchLatestBaileysVersion, makeCacheableSignalKeyStore } from '@whiskeysockets/baileys';
import type { Logger } from 'pino';
import type { Config } from './config.js';
import type { FabricaSocket, SocketLike } from './sessao.js';

export function criarFabricaBaileys(config: Config, log: Logger): FabricaSocket {
  const logDoBaileys = log.child({ origem: 'baileys' }, { level: 'warn' });
  return async ({ auth }) => {
    let versao: [number, number, number] | undefined;
    if (config.buscarVersaoWhatsapp) {
      try {
        versao = (await fetchLatestBaileysVersion()).version;
      } catch {
        log.warn('Nao foi possivel buscar a versao atual do WhatsApp Web; usando a versao embutida');
      }
    }
    const socket = makeWASocket({
      ...(versao ? { version: versao } : {}),
      auth: {
        creds: auth.state.creds,
        keys: makeCacheableSignalKeyStore(auth.state.keys, logDoBaileys as never)
      },
      logger: logDoBaileys as never,
      browser: Browsers.ubuntu('Chrome'),
      markOnlineOnConnect: false,
      syncFullHistory: false,
      shouldSyncHistoryMessage: () => false,
      generateHighQualityLinkPreview: false,
      getMessage: async () => undefined
    });
    return socket as unknown as SocketLike;
  };
}
