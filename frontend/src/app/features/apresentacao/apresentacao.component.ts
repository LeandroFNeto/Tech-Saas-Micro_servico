import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ChatFlutuanteComponent } from '../../shared/components/chat-flutuante/chat-flutuante.component';
import contentData from '../../../../../especificacao/front/apresentacao.content.json';
import designData from '../../../../../especificacao/front/telas.design.json';

@Component({
  selector: 'app-apresentacao',
  imports: [RouterLink, ChatFlutuanteComponent],
  templateUrl: './apresentacao.component.html'
})
export class ApresentacaoComponent {
  readonly conteudo = contentData.apresentacao;
  readonly design = designData.telas.apresentacao;
  readonly linkWhatsapp = `https://wa.me/${contentData.apresentacao.contato.whatsapp}`;

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl(this.auth.rotaInicial());
    }
  }

  /** Rola até "Como funciona" sem trocar a rota e leva o foco para a seção. */
  irParaComo(evento: Event, secao: HTMLElement): void {
    evento.preventDefault();
    const reduzirMovimento = typeof window !== 'undefined'
      && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    secao.scrollIntoView({ behavior: reduzirMovimento ? 'auto' : 'smooth', block: 'start' });
    secao.focus({ preventScroll: true });
  }
}
