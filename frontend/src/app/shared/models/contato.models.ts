export interface ContatoResumoDTO {
  id: number;
  nome: string;
  telefoneMascarado: string;
  primeiraConversaEm?: string | null;
  ultimaConversaEm?: string | null;
  naoContatar: boolean;
  origem: string;
}

export interface NumeroContatoDTO {
  numero: string;
}
