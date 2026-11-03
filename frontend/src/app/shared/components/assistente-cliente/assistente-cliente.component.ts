import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
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
export class AssistenteClienteComponent implements OnInit {
  private readonly assistenteService = inject(AssistenteTutoriaService);

  @Input() modoInicial: 'SUPORTE' | 'TESTE_VIRTUAL' = 'SUPORTE';

  modo = signal<'SUPORTE' | 'TESTE_VIRTUAL'>('SUPORTE');
  mensagens = signal<MensagemClienteChat[]>([]);
  mensagemInput = '';
  enviando = signal(false);
  erro = signal<string | null>(null);

  sugestoes = signal<string[]>([
    'Como conectar o WhatsApp via QR Code?',
    'Como sincronizar minha Google Agenda?',
    'Como cadastrar fotos e regras de locação?',
    'Onde vejo os tutoriais passo a passo?'
  ]);

  ngOnInit(): void {
    this.modo.set(this.modoInicial);
    this.aplicarModo(this.modoInicial);
  }

  alternarModo(novoModo: 'SUPORTE' | 'TESTE_VIRTUAL'): void {
    this.modo.set(novoModo);
    this.aplicarModo(novoModo);
  }

  private aplicarModo(modo: 'SUPORTE' | 'TESTE_VIRTUAL'): void {
    if (modo === 'TESTE_VIRTUAL') {
      this.sugestoes.set([
        'Olá! Quais são os preços e regras?',
        'Menu de opções',
        'Onde fica localizado o espaço?',
        'Pode me enviar fotos do local?'
      ]);
      this.mensagens.set([
        {
          origem: 'ia',
          texto: 'Sessão Virtual de Testes ativada! Digite comandos como "oi", "menu", "preço" ou "regras" para testar o atendimento do seu bot sem precisar de WhatsApp físico.',
          dataHora: new Date()
        }
      ]);
    } else {
      this.sugestoes.set([
        'Como conectar o WhatsApp via QR Code?',
        'Como sincronizar minha Google Agenda?',
        'Como cadastrar fotos e regras de locação?',
        'Onde vejo os tutoriais passo a passo?'
      ]);
      this.mensagens.set([
        {
          origem: 'ia',
          texto: 'Olá! Sou o assistente de suporte e tutoria da sua empresa. Como posso te orientar hoje na operação do sistema?',
          dataHora: new Date()
        }
      ]);
    }
  }

  usarSugestao(texto: string): void {
    this.mensagemInput = texto;
    this.enviar();
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

    this.assistenteService.conversarCliente({ mensagem: texto, modo: this.modo() }).subscribe({
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
      error: () => {
        this.erro.set('Não foi possível obter resposta do assistente no momento. Tente novamente.');
        this.enviando.set(false);
      }
    });
  }
}
