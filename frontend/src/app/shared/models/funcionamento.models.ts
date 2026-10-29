export type PerfilFuncionamento =
  | 'SEGUNDA'
  | 'TERCA'
  | 'QUARTA'
  | 'QUINTA'
  | 'SEXTA'
  | 'SABADO'
  | 'DOMINGO'
  | 'FERIADO';

export interface HorarioFuncionamentoDTO {
  perfil: PerfilFuncionamento;
  fechado: boolean;
  abre?: string | null;
  fecha?: string | null;
  duracaoMinutos: number | null;
}

export interface DataEspecialDTO {
  id: number;
  data: string;
  fechado: boolean;
  abre?: string | null;
  fecha?: string | null;
  motivo?: string | null;
  reservasNaData?: string[] | null;
}

export interface DataEspecialCreateDTO {
  data: string;
  fechado: boolean;
  abre?: string;
  fecha?: string;
  motivo?: string;
}

export interface FuncionamentoDTO {
  duracaoAtendimentoMinutos: number;
  intervaloEntreAtendimentosMinutos: number;
  uf?: string | null;
  horarios: HorarioFuncionamentoDTO[];
  diasEspeciais: DataEspecialDTO[];
}

export interface FuncionamentoUpdateDTO {
  duracaoAtendimentoMinutos: number;
  intervaloEntreAtendimentosMinutos: number;
  uf: string;
  horarios: HorarioFuncionamentoDTO[];
}
