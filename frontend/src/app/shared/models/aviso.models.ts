/** Espelha AvisoDTO do contrato OpenAPI. */
export type TipoAviso = 'MANUTENCAO' | 'TUTORIAL' | 'PRECO' | 'COMUNICADO';

export interface AvisoDTO {
  id?: number;
  tipo: TipoAviso;
  titulo: string;
  conteudo: string;
  link?: string | null;
  ativo: boolean;
}

export const TIPOS_AVISO: { codigo: TipoAviso; label: string }[] = [
  { codigo: 'MANUTENCAO', label: 'Manutenção' },
  { codigo: 'TUTORIAL', label: 'Tutorial' },
  { codigo: 'PRECO', label: 'Preço' },
  { codigo: 'COMUNICADO', label: 'Comunicado' }
];

export function rotuloTipoAviso(tipo: TipoAviso): string {
  return TIPOS_AVISO.find((item) => item.codigo === tipo)?.label ?? tipo;
}
