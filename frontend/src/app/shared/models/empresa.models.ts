export interface LembreteConfirmacaoDTO {
  modo?: 'DESLIGADO' | 'SUGERIDO' | 'AUTOMATICO';
  diasAntes?: number;
  horarioEnvio?: string;
  texto?: string;
}

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
  linkGaleria?: string;
  modulosIniciais?: string[];
}

export interface EmpresaUpdateDTO {
  nome?: string;
  mensagemSaudacao?: string;
  tabelaDePrecos?: string;
  linkGoogleMaps?: string;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  linkGaleria?: string;
  permiteReservaAutomatica?: boolean;
  locacaoPorHora?: boolean;
  regrasLocacao?: string;
  modulosMenu?: ModuloMenuDTO[];
  cidadePrevisao?: string;
  previsaoTempoAtiva?: boolean;
  lembrete?: LembreteConfirmacaoDTO;
}

export interface EmpresaUpdateAdminDTO {
  sessaoWhatsapp?: string;
  locacaoPorHora?: boolean;
  googleCalendarId?: string | null;
  permiteReservaAutomatica?: boolean;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  linkGaleria?: string;
  modulosAtivos?: string[];
}

export interface EmpresaResponseDTO {
  id: number;
  nome: string;
  sessaoWhatsapp: string;
  statusSessao: string;
  linkGoogleMaps: string;
  linkFotoPrincipal?: string;
  urlsGaleria?: string[];
  linkGaleria?: string;
  permiteReservaAutomatica?: boolean;
  locacaoPorHora?: boolean;
  regrasLocacao?: string;
  modulosMenu?: ModuloMenuDTO[];
  cidadePrevisao?: string;
  cidadePrevisaoResolvida?: string;
  previsaoTempoAtiva?: boolean;
  lembrete?: LembreteConfirmacaoDTO;
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
  linkGaleria?: string;
  locacaoPorHora?: boolean;
  googleCalendarId?: string;
  usaIA?: boolean;
  permiteReservaAutomatica?: boolean;
  regrasLocacao?: string;
  modulosAtivos: string[];
  modulosMenu: ModuloMenuDTO[];
  cidadePrevisao?: string;
  cidadePrevisaoResolvida?: string;
  previsaoTempoAtiva?: boolean;
  lembrete?: LembreteConfirmacaoDTO;
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
    codigo: 'RESERVAR',
    label: 'Reserva',
    descricao: 'Agenda uma visita ou locação pelo bot',
    textoPadrao: 'Fazer reserva'
  }
];

export const CATALOGO_MODULOS: CatalogoModulo[] = [
  {
    codigo: 'VER_FOTOS',
    label: 'Fotos do espaço',
    descricao: 'Envia até 5 fotos cadastradas quando o cliente pedir'
  },
  {
    codigo: 'VER_LOCALIZACAO',
    label: 'Localização no mapa',
    descricao: 'Envia o link do Google Maps configurado'
  },
  {
    codigo: 'MENU_CARDAPIO',
    label: 'Tabela de preços',
    descricao: 'Mostra o texto de preços cadastrado'
  },
  {
    codigo: 'RESERVAR',
    label: 'Agendamento / Reserva',
    descricao: 'Permite ao cliente consultar disponibilidade e agendar'
  }
];

export const MODULOS_DISPONIVEIS = CATALOGO_MODULOS;
