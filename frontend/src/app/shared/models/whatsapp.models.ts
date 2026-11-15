export type StatusSessaoWhatsapp = 'CONNECTED' | 'QRCODE' | 'INITIALIZING' | 'DISCONNECTED';

export interface SessaoStatusResponseDTO {
  sessao: string;
  status: StatusSessaoWhatsapp;
  qrcodeBase64: string | null;
  sessaoEncerrada?: boolean;
}

export interface CodigoPareamentoRequestDTO {
  telefone: string;
}

export interface CodigoPareamentoResponseDTO {
  sessao: string;
  codigo: string;
}
