import pino, { type DestinationStream, type Logger } from 'pino';
import { sanear } from './mascara.js';

export function criarLogger(nivel: string, destino?: DestinationStream): Logger {
  const opcoes = {
    level: nivel,
    base: { servico: 'baileys-gateway' },
    hooks: {
      logMethod(this: Logger, argumentos: unknown[], metodo: (...args: unknown[]) => void) {
        const limpos = argumentos.map((argumento) => sanear(argumento));
        metodo.apply(this, limpos);
      }
    }
  };
  return (destino ? pino(opcoes as never, destino) : pino(opcoes as never)) as Logger;
}

export type { Logger };
