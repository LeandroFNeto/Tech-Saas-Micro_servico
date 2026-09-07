import { Injectable } from '@angular/core';
import { AvisoPainel } from '../../shared/models/empresa.models';

@Injectable({ providedIn: 'root' })
export class AvisoService {
  listar(): AvisoPainel[] {
    return [
      {
        id: 'upgrade',
        tipo: 'upgrade',
        titulo: 'Faça upgrade do plano',
        texto: 'Libere IA Gemini e Agenda Google para o seu bot atender 24h sem fila.',
        cta: 'Ver planos',
        href: 'https://example.com/planos'
      },
      {
        id: 'tutorial',
        tipo: 'tutorial',
        titulo: 'Como conectar o WhatsApp',
        texto: 'Abra a sessão no WPPConnect, escaneie o QR Code e volte aqui para conferir o status.',
        cta: 'Ver tutorial',
        href: 'https://example.com/tutorial-whatsapp'
      },
      {
        id: 'comunicado',
        tipo: 'comunicado',
        titulo: 'Comunicado',
        texto: 'Manutenção programada no WPPConnect neste domingo, das 2h às 4h. O bot pode ficar offline nesse intervalo.'
      }
    ];
  }
}
