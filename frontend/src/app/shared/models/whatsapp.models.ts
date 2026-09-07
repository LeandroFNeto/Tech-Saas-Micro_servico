export type StatusSessaoWhatsapp = 'CONNECTED' | 'QRCODE' | 'DISCONNECTED';

export interface SessaoStatusResponseDTO {
  sessao: string;
  status: StatusSessaoWhatsapp;
  qrcodeBase64: string | null;
}
