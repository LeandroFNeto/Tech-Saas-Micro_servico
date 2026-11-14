import { copyFile, mkdir, open, readFile, rename, rm, stat } from 'node:fs/promises';
import { dirname } from 'node:path';

const trancas = new Map<string, Promise<unknown>>();

export function comTranca<T>(caminho: string, acao: () => Promise<T>): Promise<T> {
  const anterior = trancas.get(caminho) ?? Promise.resolve();
  const proxima = anterior.then(acao, acao);
  const limpeza = proxima.catch(() => undefined);
  trancas.set(caminho, limpeza);
  void limpeza.then(() => {
    if (trancas.get(caminho) === limpeza) {
      trancas.delete(caminho);
    }
  });
  return proxima;
}

async function existe(caminho: string): Promise<boolean> {
  try {
    await stat(caminho);
    return true;
  } catch {
    return false;
  }
}

export async function gravarAtomico(caminho: string, conteudo: string): Promise<void> {
  await comTranca(caminho, async () => {
    await mkdir(dirname(caminho), { recursive: true, mode: 0o700 });
    const temporario = `${caminho}.${process.pid}.tmp`;
    const arquivo = await open(temporario, 'w', 0o600);
    try {
      await arquivo.writeFile(conteudo, 'utf8');
      await arquivo.sync();
    } finally {
      await arquivo.close();
    }
    if (await existe(caminho)) {
      await copyFile(caminho, `${caminho}.bak`);
    }
    await rename(temporario, caminho);
  });
}

export async function lerComReserva<T>(caminho: string, reviver?: (chave: string, valor: any) => any): Promise<T | null> {
  for (const candidato of [caminho, `${caminho}.bak`]) {
    try {
      const texto = await readFile(candidato, 'utf8');
      return JSON.parse(texto, reviver) as T;
    } catch {
      continue;
    }
  }
  return null;
}

export async function removerComReserva(caminho: string): Promise<void> {
  await comTranca(caminho, async () => {
    await rm(caminho, { force: true });
    await rm(`${caminho}.bak`, { force: true });
  });
}
