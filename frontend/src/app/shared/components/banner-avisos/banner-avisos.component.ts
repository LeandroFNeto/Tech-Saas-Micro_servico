import { Component, Input } from '@angular/core';
import { AvisoPainel } from '../../models/empresa.models';

@Component({
  selector: 'app-banner-avisos',
  templateUrl: './banner-avisos.component.html'
})
export class BannerAvisosComponent {
  @Input({ required: true }) avisos: AvisoPainel[] = [];
  @Input() compacto = false;

  estilo(tipo: AvisoPainel['tipo']): string {
    if (tipo === 'upgrade') {
      return 'from-emerald-600 to-teal-700';
    }
    if (tipo === 'tutorial') {
      return 'from-sky-600 to-indigo-700';
    }
    return 'from-amber-500 to-orange-600';
  }
}
