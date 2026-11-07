import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import {
  AssistenteChatResponseDTO,
  DiagnosticoResponseDTO
} from '../../../shared/models/assistente.models';

interface MensagemChat {
  origem: 'usuario' | 'ia';
  texto: string;
  dataHora: Date;
}

@Component({
  selector: 'app-laboratorio-testes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './laboratorio-testes.component.html'
})
export class LaboratorioTestesComponent implements OnInit {
  private readonly assistenteService = inject(AssistenteTutoriaService);
  private readonly destroyRef = inject(DestroyRef);
  private chatSub: Subscription | null = null;

  modo = signal<'DIAGNOSTICO' | 'ATENDIMENTO_SANDBOX'>('DIAGNOSTICO');
  carregandoDiagnostico = signal(false);
  diagnostico = signal<DiagnosticoResponseDTO | null>(null);
  erroDiagnostico = signal<string | null>(null);

  mensagens = signal<MensagemChat[]>([]);
  mensagemInput = '';
  enviandoMensagem = signal(false);
  sugestoes = signal<string[]>([
    'Como conectar o WhatsApp via QR Code?',
    'Como configurar locação por hora vs diária?',
    'Como cadastrar avisos e tutoriais no painel?',
    'Qual a diferença entre perfil Admin e Responsável?'
  ]);

  ngOnInit(): void {
    this.executarDiagnostico();
    this.mensagens.set([
      {
        origem: 'ia',
        texto: 'Olá! Sou o assistente inteligente do GAMB. Estou pronto para tirar dúvidas, orientar configurações ou simular atendimentos de clientes.',
        dataHora: new Date()
      }
    ]);
  }

  alternarModo(novoModo: 'DIAGNOSTICO' | 'ATENDIMENTO_SANDBOX'): void {
    this.chatSub?.unsubscribe();
    this.chatSub = null;
    this.enviandoMensagem.set(false);
    this.modo.set(novoModo);
    if (novoModo === 'ATENDIMENTO_SANDBOX') {
      this.sugestoes.set([
        'Quais são os horários e regras de locação?',
        'Como funciona a reserva integrada à agenda?',
        'Tem algum aviso ativo no momento?'
      ]);
    } else {
      this.sugestoes.set([
        'Como conectar o WhatsApp via QR Code?',
        'Como configurar locação por hora vs diária?',
        'Como cadastrar avisos e tutoriais no painel?',
        'Qual a diferença entre perfil Admin e Responsável?'
      ]);
    }
  }

  executarDiagnostico(): void {
    this.carregandoDiagnostico.set(true);
    this.erroDiagnostico.set(null);
    this.assistenteService
      .executarDiagnostico()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resp) => {
          this.diagnostico.set(resp);
          this.carregandoDiagnostico.set(false);
        },
        error: () => {
          this.erroDiagnostico.set('Não foi possível obter o diagnóstico dos módulos.');
          this.carregandoDiagnostico.set(false);
        }
      });
  }

  enviarMensagem(): void {
    const texto = this.mensagemInput.trim();
    if (!texto || this.enviandoMensagem()) {
      return;
    }

    const novaMsgUsuario: MensagemChat = {
      origem: 'usuario',
      texto,
      dataHora: new Date()
    };
    this.mensagens.update((msgs) => [...msgs, novaMsgUsuario]);
    this.mensagemInput = '';
    this.enviandoMensagem.set(true);

    this.chatSub?.unsubscribe();
    this.chatSub = this.assistenteService
      .conversar({ mensagem: texto, modo: this.modo() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resp: AssistenteChatResponseDTO) => {
          const novaMsgIA: MensagemChat = {
            origem: 'ia',
            texto: resp.resposta,
            dataHora: new Date()
          };
          this.mensagens.update((msgs) => [...msgs, novaMsgIA]);
          if (resp.sugestoes && resp.sugestoes.length > 0) {
            this.sugestoes.set(resp.sugestoes);
          }
          this.enviandoMensagem.set(false);
          this.chatSub = null;
        },
        error: () => {
          const msgErro: MensagemChat = {
            origem: 'ia',
            texto: 'Desculpe, ocorreu uma instabilidade momentânea ao consultar a IA.',
            dataHora: new Date()
          };
          this.mensagens.update((msgs) => [...msgs, msgErro]);
          this.enviandoMensagem.set(false);
          this.chatSub = null;
        }
      });
  }

  selecionarSugestao(sugestao: string): void {
    this.mensagemInput = sugestao;
    this.enviarMensagem();
  }

  statusClasse(status: string): string {
    switch (status) {
      case 'OPERACIONAL':
        return 'border-emerald-200 bg-emerald-50 text-emerald-800';
      case 'ATENCAO':
      case 'DEGRADADO':
        return 'border-amber-200 bg-amber-50 text-amber-800';
      default:
        return 'border-rose-200 bg-rose-50 text-rose-800';
    }
  }

  indicadorDotClasse(status: string): string {
    switch (status) {
      case 'OPERACIONAL':
        return 'bg-emerald-500';
      case 'ATENCAO':
      case 'DEGRADADO':
        return 'bg-amber-500';
      default:
        return 'bg-rose-500';
    }
  }
}
