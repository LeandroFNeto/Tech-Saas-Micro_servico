export interface OpcoesFila {
  intervaloMinMs?: number;
  intervaloMaxMs?: number;
  maximo?: number;
  aleatorio?: () => number;
  dormir?: (ms: number) => Promise<void>;
  agora?: () => number;
}

export type Tarefa = () => Promise<void>;

export class FilaDeEnvio {
  private readonly pendentes: Tarefa[] = [];
  private processando = false;
  private ultimoFim = 0;
  private readonly intervaloMin: number;
  private readonly intervaloMax: number;
  private readonly maximo: number;
  private readonly aleatorio: () => number;
  private readonly dormir: (ms: number) => Promise<void>;
  private readonly agora: () => number;
  private ociosa: Array<() => void> = [];

  constructor(opcoes: OpcoesFila = {}) {
    this.intervaloMin = opcoes.intervaloMinMs ?? 1000;
    this.intervaloMax = opcoes.intervaloMaxMs ?? 2500;
    this.maximo = opcoes.maximo ?? 50;
    this.aleatorio = opcoes.aleatorio ?? Math.random;
    this.dormir = opcoes.dormir ?? ((ms) => new Promise((resolver) => setTimeout(resolver, ms)));
    this.agora = opcoes.agora ?? (() => Date.now());
  }

  get tamanho(): number {
    return this.pendentes.length + (this.processando ? 1 : 0);
  }

  enfileirar(tarefa: Tarefa): boolean {
    if (this.tamanho >= this.maximo) {
      return false;
    }
    this.pendentes.push(tarefa);
    void this.processar();
    return true;
  }

  aguardarOciosa(): Promise<void> {
    if (!this.processando && this.pendentes.length === 0) {
      return Promise.resolve();
    }
    return new Promise((resolver) => this.ociosa.push(resolver));
  }

  descartarTudo(): void {
    this.pendentes.length = 0;
  }

  private async processar(): Promise<void> {
    if (this.processando) {
      return;
    }
    this.processando = true;
    try {
      while (this.pendentes.length > 0) {
        const tarefa = this.pendentes.shift() as Tarefa;
        const espera = this.intervaloMin + this.aleatorio() * (this.intervaloMax - this.intervaloMin);
        const restante = this.ultimoFim + espera - this.agora();
        if (this.ultimoFim > 0 && restante > 0) {
          await this.dormir(restante);
        }
        await Promise.resolve().then(tarefa).catch(() => undefined);
        this.ultimoFim = this.agora();
      }
    } finally {
      this.processando = false;
      const aguardando = this.ociosa;
      this.ociosa = [];
      aguardando.forEach((resolver) => resolver());
    }
  }
}
