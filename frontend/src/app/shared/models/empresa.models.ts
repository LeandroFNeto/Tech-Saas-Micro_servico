/** Espelha EmpresaCreateDTO do contrato OpenAPI. */
export interface EmpresaCreateDTO {
  nome: string;
  sessaoWhatsapp: string;
  ramoDeAtuacao?: string;
  precoBase?: number | null;
  googleCalendarId?: string | null;
  email?: string;
  senha: string;
  permiteReservaAutomatica?: boolean;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  modulosIniciais?: string[];
}

/** Espelha EmpresaUpdateDTO — painel do cliente. */
export interface EmpresaUpdateDTO {
  nome?: string;
  mensagemSaudacao?: string;
  tabelaDePrecos?: string;
  linkGoogleMaps?: string;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  permiteReservaAutomatica?: boolean;
  locacaoPorHora?: boolean;
  regrasLocacao?: string;
  modulosMenu?: ModuloMenuDTO[];
}

/** Espelha EmpresaUpdateAdminDTO — painel master. */
export interface EmpresaUpdateAdminDTO {
  sessaoWhatsapp?: string;
  locacaoPorHora?: boolean;
  googleCalendarId?: string | null;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  modulosAtivos?: string[];
}

/** Espelha EmpresaResponseDTO. */
export interface EmpresaResponseDTO {
  id: number;
  nome: string;
  sessaoWhatsapp: string;
  statusSessao: string;
  linkGoogleMaps: string;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  permiteReservaAutomatica?: boolean;
  locacaoPorHora?: boolean;
  regrasLocacao?: string;
  modulosMenu?: ModuloMenuDTO[];
  atualizadoEm: string;
}

export interface ModuloMenuDTO {
  codigoAcao: string;
  textoMenu?: string;
  ordemExibicao?: number;
  ativo?: boolean;
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
  urlsGaleria?: string[];
  locacaoPorHora?: boolean;
  googleCalendarId?: string;
  usaIA?: boolean;
  permiteReservaAutomatica?: boolean;
  regrasLocacao?: string;
  modulosAtivos: string[];
  modulosMenu: ModuloMenuDTO[];
  atualizadoEm?: string;
}

export interface CatalogoModulo {
  codigo: string;
  label: string;
  descricao: string;
}

export interface CatalogoMenuBot {
  codigo: string;
  label: string;
  descricao: string;
  textoPadrao: string;
}

export const MAX_URLS_GALERIA = 5;

export const MODULOS_MENU_BOT: CatalogoMenuBot[] = [
  {
    codigo: 'VER_FOTOS',
    label: 'Fotos',
    descricao: 'Envia fotos do espaço pelo WhatsApp',
    textoPadrao: 'Ver fotos do espaço'
  },
  {
    codigo: 'VER_LOCALIZACAO',
    label: 'Localização',
    descricao: 'Envia o link do Google Maps no menu do bot',
    textoPadrao: 'Ver localização'
  },
  {
    codigo: 'MENU_CARDAPIO',
    label: 'Preços',
    descricao: 'Envia a mensagem de preços cadastrada pelo WhatsApp',
    textoPadrao: 'Ver preços'
  },
  {
    codigo: 'VER_REGRAS',
    label: 'Regras',
    descricao: 'Mostra as regras do local e o cancelamento',
    textoPadrao: 'Ver regras e cancelamento'
  },
  {
    codigo: 'SOLICITAR_RESERVA',
    label: 'Reserva',
    descricao: 'Opção de solicitar reserva no WhatsApp',
    textoPadrao: 'Solicitar uma reserva'
  }
];

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
    label: 'Preços',
    descricao: 'Envia a tabela de preços cadastrada pelo WhatsApp'
  }
];
