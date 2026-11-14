import QRCode from 'qrcode';

const PREFIXO = 'data:image/png;base64,';

export async function gerarQrPng(textoDoQr: string): Promise<string> {
  const url = await QRCode.toDataURL(textoDoQr, { errorCorrectionLevel: 'M', margin: 2, scale: 6 });
  return url.startsWith(PREFIXO) ? url : `${PREFIXO}${url}`;
}

export function bytesDoPng(qrBase64: string): Buffer {
  return Buffer.from(qrBase64.startsWith(PREFIXO) ? qrBase64.slice(PREFIXO.length) : qrBase64, 'base64');
}
