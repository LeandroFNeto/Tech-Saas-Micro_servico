import type { Logger } from 'pino';

export interface OpcoesWebhook {
  segredo: string;
  esperasMs?: number[];
  timeoutMs?: number;
  buscar?: typeof fetch;
  dormir?: (ms: number) => Promise<void>;
}

export type ResultadoWebhook = { entregue: true; tentativas: number } | { entregue: false; tentativas: number; motivo: string };

const ESPERAS_PADRAO = [1000, 2000, 4000];

export class DespachanteWebhook {
  private readonly esperas: number[];
  private readonly timeoutMs: number;
  private readonly buscar: typeof fetch;
  private readonly dormir: (ms: number) => Promise<void>;

  constructor(
    private readonly opcoes: OpcoesWebhook,
    private readonly log: Logger
  ) {
    this.esperas = opcoes.esperasMs ?? ESPERAS_PADRAO;
    this.timeoutMs = opcoes.timeoutMs ?? 60000;
    this.buscar = opcoes.buscar ?? fetch;
    this.dormir = opcoes.dormir ?? ((ms) => new Promise((resolver) => setTimeout(resolver, ms)));
  }

  async postar(url: string, sessao: string, payload: unknown): Promise<ResultadoWebhook> {
    const corpo = JSON.stringify(payload);
    let tentativa = 0;
    for (;;) {
      tentativa++;
      const resultado = await this.tentar(url, corpo);
      if (resultado.ok) {
        return { entregue: true, tentativas: tentativa };
      }
      if (!resultado.repetir || tentativa > this.esperas.length) {
        this.log.warn({ sessao, motivo: resultado.motivo, tentativas: tentativa }, 'Webhook nao entregue');
        return { entregue: false, tentativas: tentativa, motivo: resultado.motivo };
      }
      await this.dormir(this.esperas[tentativa - 1]);
    }
  }

  private async tentar(url: string, corpo: string): Promise<{ ok: true } | { ok: false; repetir: boolean; motivo: string }> {
    const controle = new AbortController();
    const relogio = setTimeout(() => controle.abort(), this.timeoutMs);
    try {
      const resposta = await this.buscar(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-webhook-token': this.opcoes.segredo
        },
        body: corpo,
        signal: controle.signal
      });
      if (resposta.ok) {
        return { ok: true };
      }
      if (resposta.status >= 500) {
        return { ok: false, repetir: true, motivo: `http_${resposta.status}` };
      }
      return { ok: false, repetir: false, motivo: `http_${resposta.status}` };
    } catch (erro) {
      const nome = erro instanceof Error ? erro.name : 'erro';
      return { ok: false, repetir: true, motivo: `rede_${nome}` };
    } finally {
      clearTimeout(relogio);
    }
  }
}
