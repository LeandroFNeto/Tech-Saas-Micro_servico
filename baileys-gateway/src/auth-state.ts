import { BufferJSON, initAuthCreds, proto } from '@whiskeysockets/baileys';
import type { AuthenticationCreds, AuthenticationState, SignalDataTypeMap } from '@whiskeysockets/baileys';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { gravarAtomico, lerComReserva, removerComReserva } from './arquivos.js';

export function credenciaisPareadas(creds: { registered?: boolean; me?: { id?: string } | null; account?: unknown } | null | undefined): boolean {
  return Boolean(creds?.me?.id && (creds.registered === true || creds.account));
}

export interface EstadoDeAutenticacao {
  state: AuthenticationState;
  salvarCredenciais: () => Promise<void>;
  registrada: () => boolean;
}

function nomeDoArquivo(arquivo: string): string {
  return `${arquivo.replace(/\//g, '__').replace(/:/g, '-')}.json`;
}

export function temCredenciaisSalvas(arquivos: string[]): boolean {
  return arquivos.includes('creds.json') || arquivos.includes('creds.json.bak');
}

export async function usarAutenticacaoAtomica(pasta: string): Promise<EstadoDeAutenticacao> {
  await mkdir(pasta, { recursive: true, mode: 0o700 });

  const gravar = (dados: unknown, arquivo: string): Promise<void> =>
    gravarAtomico(join(pasta, nomeDoArquivo(arquivo)), JSON.stringify(dados, BufferJSON.replacer));

  const ler = (arquivo: string): Promise<any> =>
    lerComReserva<any>(join(pasta, nomeDoArquivo(arquivo)), BufferJSON.reviver);

  const remover = (arquivo: string): Promise<void> => removerComReserva(join(pasta, nomeDoArquivo(arquivo)));

  const creds: AuthenticationCreds = (await ler('creds')) ?? initAuthCreds();

  return {
    state: {
      creds,
      keys: {
        get: async (tipo, ids) => {
          const dados: { [id: string]: SignalDataTypeMap[typeof tipo] } = {};
          await Promise.all(
            ids.map(async (id) => {
              let valor = await ler(`${tipo}-${id}`);
              if (tipo === 'app-state-sync-key' && valor) {
                valor = proto.Message.AppStateSyncKeyData.fromObject(valor);
              }
              dados[id] = valor;
            })
          );
          return dados;
        },
        set: async (dados) => {
          const tarefas: Promise<void>[] = [];
          for (const categoria of Object.keys(dados)) {
            const entradas = (dados as any)[categoria] as Record<string, unknown>;
            for (const id of Object.keys(entradas)) {
              const valor = entradas[id];
              const arquivo = `${categoria}-${id}`;
              tarefas.push(valor ? gravar(valor, arquivo) : remover(arquivo));
            }
          }
          await Promise.all(tarefas);
        }
      }
    },
    salvarCredenciais: () => gravar(creds, 'creds'),
    registrada: () => credenciaisPareadas(creds)
  };
}
