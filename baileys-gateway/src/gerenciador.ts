import { readdir, rm } from 'node:fs/promises';
import { join } from 'node:path';
import { lerComReserva } from './arquivos.js';
import { credenciaisPareadas } from './auth-state.js';
import { webhookPermitido } from './config.js';
import { sessaoValida } from './seguranca.js';
import { Sessao, type DependenciasSessao } from './sessao.js';

export interface SaudeSessoes {
  conectadas: number;
  aguardandoQr: number;
  desconectadas: string[];
  nomesConectadas: string[];
}

export class GerenciadorSessoes {
  private readonly sessoes = new Map<string, Sessao>();

  constructor(private readonly deps: DependenciasSessao) {}

  obter(nome: string): Sessao | undefined {
    return this.sessoes.get(nome);
  }

  private obterOuCriar(nome: string): Sessao {
    let sessao = this.sessoes.get(nome);
    if (!sessao) {
      sessao = new Sessao(nome, this.deps);
      this.sessoes.set(nome, sessao);
    }
    return sessao;
  }

  async iniciar(nome: string, webhook?: string): Promise<Sessao> {
    const sessao = this.obterOuCriar(nome);
    if (webhook && webhookPermitido(webhook, this.deps.config.hostsWebhookPermitidos)) {
      sessao.definirWebhook(webhook);
      await sessao.gravarMeta();
    }
    await sessao.iniciar();
    return sessao;
  }

  async fechar(nome: string): Promise<void> {
    const sessao = this.sessoes.get(nome);
    if (sessao) {
      await sessao.fechar();
      this.sessoes.delete(nome);
      return;
    }
    await rm(join(this.deps.config.pastaAutenticacao, nome), { recursive: true, force: true });
  }

  async restaurar(): Promise<number> {
    let entradas: string[] = [];
    try {
      entradas = await readdir(this.deps.config.pastaAutenticacao);
    } catch {
      return 0;
    }
    let restauradas = 0;
    for (const nome of entradas) {
      if (!sessaoValida(nome)) {
        continue;
      }
      const pasta = join(this.deps.config.pastaAutenticacao, nome);
      const creds = await lerComReserva<{ registered?: boolean; me?: { id?: string } | null; account?: unknown }>(join(pasta, 'creds.json'));
      if (!credenciaisPareadas(creds)) {
        continue;
      }
      const sessao = this.obterOuCriar(nome);
      sessao.definirWebhook(await this.lerWebhookGravado(pasta));
      await sessao.iniciar();
      restauradas++;
    }
    this.deps.log.info({ restauradas }, 'Sessoes restauradas no boot');
    return restauradas;
  }

  private async lerWebhookGravado(pasta: string): Promise<string | null> {
    const meta = await lerComReserva<{ webhook?: string }>(join(pasta, 'meta.json'));
    if (meta?.webhook && webhookPermitido(meta.webhook, this.deps.config.hostsWebhookPermitidos)) {
      return meta.webhook;
    }
    return null;
  }

  saude(): SaudeSessoes {
    const saude: SaudeSessoes = { conectadas: 0, aguardandoQr: 0, desconectadas: [], nomesConectadas: [] };
    for (const [nome, sessao] of this.sessoes) {
      if (sessao.estado === 'CONNECTED') {
        saude.conectadas++;
        saude.nomesConectadas.push(nome);
      } else if (sessao.estado === 'QRCODE') {
        saude.aguardandoQr++;
      } else {
        saude.desconectadas.push(nome);
      }
    }
    saude.desconectadas.sort();
    saude.nomesConectadas.sort();
    return saude;
  }

  async parar(): Promise<void> {
    await Promise.all([...this.sessoes.values()].map((sessao) => sessao.parar()));
  }
}
