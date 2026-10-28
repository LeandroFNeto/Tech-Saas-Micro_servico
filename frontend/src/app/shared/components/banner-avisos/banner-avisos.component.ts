import { Component, Input } from '@angular/core';
import { AvisoDTO, TipoAviso, rotuloTipoAviso } from '../../models/aviso.models';

@Component({
  selector: 'app-banner-avisos',
  templateUrl: './banner-avisos.component.html'
})
export class BannerAvisosComponent {
  @Input({ required: true }) avisos: AvisoDTO[] = [];
  @Input() compacto = false;

  rotulo(tipo: TipoAviso): string {
    return rotuloTipoAviso(tipo);
  }

  cta(tipo: TipoAviso): string {
    if (tipo === 'TUTORIAL') {
      return 'Ver tutorial';
    }
    if (tipo === 'PRECO') {
      return 'Ver preços';
    }
    return 'Saiba mais';
  }

  manutencoes(): AvisoDTO[] {
    return this.avisos.filter((aviso) => aviso.tipo === 'MANUTENCAO');
  }

  demais(): AvisoDTO[] {
    return this.avisos.filter((aviso) => aviso.tipo !== 'MANUTENCAO');
  }

  /** Só visual: fundo e borda do cartão lateral por tipo. */
  cardTipo(tipo: TipoAviso): string {
    if (tipo === 'PRECO') {
      return 'border-accent-200 bg-accent-50';
    }
    if (tipo === 'MANUTENCAO') {
      return 'border-red-200 bg-red-50';
    }
    return 'border-brand-200 bg-white';
  }

  /** Só visual: cor do rótulo do tipo. */
  textoTipo(tipo: TipoAviso): string {
    if (tipo === 'TUTORIAL') {
      return 'text-brand-700';
    }
    if (tipo === 'PRECO') {
      return 'text-accent-800';
    }
    if (tipo === 'MANUTENCAO') {
      return 'text-red-700';
    }
    return 'text-brand-600';
  }
}
