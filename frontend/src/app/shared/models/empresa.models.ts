/** Espelha EmpresaCreateDTO do contrato OpenAPI. */
export interface EmpresaCreateDTO {
  nome: string;
  sessaoWhatsapp: string;
  ramoDeAtuacao?: string;
  precoBase?: number | null;
  googleCalendarId?: string | null;
  modulosIniciais?: string[];
}

/** Espelha EmpresaUpdateDTO — painel do cliente. */
export interface EmpresaUpdateDTO {
  nome?: string;
  mensagemSaudacao?: string;
  tabelaDePrecos?: string;
  linkGoogleMaps?: string;
  linkFotoPrincipal?: string;
  linkGaleria?: string;
}

/** Espelha EmpresaUpdateAdminDTO — painel master. */
export interface EmpresaUpdateAdminDTO {
  sessaoWhatsapp?: string;
  locacaoPorHora?: boolean;
  googleCalendarId?: string | null;
  modulosAtivos?: string[];
}

/** Espelha EmpresaResponseDTO. */
export interface EmpresaResponseDTO {
  id: number;
  nome: string;
  sessaoWhatsapp: string;
  statusSessao: string;
  linkGoogleMaps: string;
  atualizadoEm: string;
}

export interface ModuloEmpresa {
  id?: number;
  codigoAcao: string;
  textoMenu?: string;
  ordemExibicao?: number;
  ativo?: boolean;
}

/** Visão unificada para as telas (HAL + DTO de resposta). */
export interface EmpresaView {
  id?: number;
  nome: string;
  sessaoWhatsapp: string;
  statusSessao?: string;
  ramoDeAtuacao?: string;
  mensagemSaudacao?: string;
  tabelaDePrecos?: string;
  linkGoogleMaps?: string;
  linkFotoPrincipal?: string;
  linkGaleria?: string;
  locacaoPorHora?: boolean;
  googleCalendarId?: string;
  usaIA?: boolean;
  modulosAtivos: string[];
  atualizadoEm?: string;
}

export interface CatalogoModulo {
  codigo: string;
  label: string;
  descricao: string;
}

export const MODULOS_DISPONIVEIS: CatalogoModulo[] = [
  {
    codigo: 'IA_GEMINI',
    label: 'Módulo de IA',
    descricao: 'Atendimento com Gemini quando o cliente sai do menu padrão'
  },
  {
    codigo: 'GOOGLE_CALENDAR',
    label: 'Módulo de Agenda',
    descricao: 'Consulta e bloqueio de horários no Google Agenda'
  },
  {
    codigo: 'VER_FOTOS',
    label: 'Galeria de fotos',
    descricao: 'Envia fotos do espaço pelo WhatsApp'
  },
  {
    codigo: 'PESQUISAR_DATA',
    label: 'Pesquisa de datas',
    descricao: 'Fluxo de disponibilidade e reserva'
  },
  {
    codigo: 'MENU_CARDAPIO',
    label: 'Menu / Cardápio',
    descricao: 'Menu dinâmico montado a partir de ModuloEmpresa'
  }
];

export interface AvisoPainel {
  id: string;
  tipo: 'upgrade' | 'tutorial' | 'comunicado';
  titulo: string;
  texto: string;
  cta?: string;
  href?: string;
}
