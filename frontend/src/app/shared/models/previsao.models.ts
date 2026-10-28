export type RiscoDesmarcacao = 'BAIXO' | 'MEDIO' | 'ALTO';

export interface PrevisaoDia {
  data: string;
  diaSemana: string;
  reservado: boolean;
  resumo: string;
  codigoTempo: number;
  tempMax: number;
  tempMin: number;
  chanceChuva: number;
  chuvaMm: number;
  risco: RiscoDesmarcacao;
  estimativa: boolean;
}

export interface PrevisaoTempoResponse {
  disponivel: boolean;
  reservasDisponiveis: boolean;
  atualizadoEm: string | null;
  cidade: string | null;
  dias: PrevisaoDia[];
}
