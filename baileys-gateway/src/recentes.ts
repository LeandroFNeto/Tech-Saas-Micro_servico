export class IdsRecentes {
  private readonly ids = new Map<string, number>();

  constructor(
    private readonly validadeMs: number,
    private readonly maximo: number,
    private readonly agora: () => number = () => Date.now()
  ) {}

  contem(id: string): boolean {
    this.expirar();
    return this.ids.has(id);
  }

  adicionar(id: string): void {
    this.expirar();
    this.ids.delete(id);
    this.ids.set(id, this.agora() + this.validadeMs);
    while (this.ids.size > this.maximo) {
      const maisAntigo = this.ids.keys().next().value as string;
      this.ids.delete(maisAntigo);
    }
  }

  registrarSeNovo(id: string): boolean {
    if (this.contem(id)) {
      return false;
    }
    this.adicionar(id);
    return true;
  }

  get tamanho(): number {
    this.expirar();
    return this.ids.size;
  }

  private expirar(): void {
    const limite = this.agora();
    for (const [id, expira] of this.ids) {
      if (expira > limite) {
        break;
      }
      this.ids.delete(id);
    }
  }
}

export class MapaRecente<V> {
  private readonly itens = new Map<string, { valor: V; expira: number }>();

  constructor(
    private readonly validadeMs: number,
    private readonly maximo: number,
    private readonly agora: () => number = () => Date.now()
  ) {}

  get(chave: string): V | undefined {
    const item = this.itens.get(chave);
    if (!item) {
      return undefined;
    }
    if (item.expira <= this.agora()) {
      this.itens.delete(chave);
      return undefined;
    }
    return item.valor;
  }

  has(chave: string): boolean {
    return this.get(chave) !== undefined;
  }

  set(chave: string, valor: V): void {
    this.itens.delete(chave);
    this.itens.set(chave, { valor, expira: this.agora() + this.validadeMs });
    while (this.itens.size > this.maximo) {
      const maisAntigo = this.itens.keys().next().value as string;
      this.itens.delete(maisAntigo);
    }
  }

  get tamanho(): number {
    return this.itens.size;
  }
}
