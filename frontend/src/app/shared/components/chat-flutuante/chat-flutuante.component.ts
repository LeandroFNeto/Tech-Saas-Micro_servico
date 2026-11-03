import { Component, Input, signal } from '@angular/core';
import { AssistenteClienteComponent } from '../assistente-cliente/assistente-cliente.component';

@Component({
  selector: 'app-chat-flutuante',
  standalone: true,
  imports: [AssistenteClienteComponent],
  templateUrl: './chat-flutuante.component.html'
})
export class ChatFlutuanteComponent {
  @Input() publico = false;
  @Input() acimaDasAbas = false;
  @Input() dica: string | null = null;

  readonly aberto = signal(false);

  alternar(): void {
    this.aberto.update((atual) => !atual);
  }

  fechar(): void {
    this.aberto.set(false);
  }

  get posicaoBase(): string {
    return this.acimaDasAbas ? 'bottom-[76px] lg:bottom-6' : 'bottom-6';
  }

  get posicaoPainel(): string {
    return this.acimaDasAbas ? 'bottom-[144px] lg:bottom-24' : 'bottom-24';
  }
}
