import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AssistenteChatResponseDTO } from '../../models/assistente.models';

export interface MensagemClienteChat {
  origem: 'usuario' | 'ia';
  texto: string;
  dataHora: Date;
}

@Component({
  selector: 'app-assistente-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assistente-cliente.component.html'
})
export class AssistenteClienteComponent {
  private readonly assistenteService = inject(AssistenteTutoriaService);

  @Input() publico = false;
  @Input() fechavel = false;
  @Output() fechar = new EventEmitter<void>();

  mensagens = signal<MensagemClienteChat[]>([
    {
      origem: 'ia',
      texto: 'Olá! Sou o assistente de suporte do app. Posso te mostrar onde fica cada função e como fazer uma tarefa.',
      dataHora: new Date()
    }
  ]);
  mensagemInput = '';
  enviando = signal(false);
  erro = signal<string | null>(null);

  sugestoes = signal<string[]>([
    'Como conectar o WhatsApp via QR Code?',
    'Como sincronizar minha Google Agenda?',
    'Como cadastrar fotos e regras de locação?',
    'Onde vejo os tutoriais passo a passo?'
  ]);

  usarSugestao(texto: string): void {
    this.mensagemInput = texto;
  }

  enviar(): void {
    const texto = this.mensagemInput.trim();
    if (!texto || this.enviando()) {
      return;
    }

    const msgUsuario: MensagemClienteChat = {
      origem: 'usuario',
      texto,
      dataHora: new Date()
    };
    this.mensagens.update((msgs) => [...msgs, msgUsuario]);
    this.mensagemInput = '';
    this.enviando.set(true);
    this.erro.set(null);

    const chamada = this.publico
      ? this.assistenteService.conversarPublico({ mensagem: texto })
      : this.assistenteService.conversarCliente({ mensagem: texto, modo: 'SUPORTE' });

    chamada.subscribe({
      next: (resp: AssistenteChatResponseDTO) => {
        const msgIa: MensagemClienteChat = {
          origem: 'ia',
          texto: resp.resposta,
          dataHora: new Date(resp.dataHora)
        };
        this.mensagens.update((msgs) => [...msgs, msgIa]);
        if (resp.sugestoes && resp.sugestoes.length > 0) {
          this.sugestoes.set(resp.sugestoes);
        }
        this.enviando.set(false);
      },
      error: (falha) => {
        this.erro.set(
          falha?.status === 429
            ? 'Muitas mensagens em pouco tempo. Aguarde um minuto e tente de novo.'
            : 'Não foi possível obter resposta do assistente no momento. Tente novamente.'
        );
        this.enviando.set(false);
      }
    });
  }
}
