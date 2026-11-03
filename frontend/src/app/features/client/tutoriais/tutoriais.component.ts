import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AvisoService } from '../../../core/services/aviso.service';
import { AvisoDTO } from '../../../shared/models/aviso.models';
import { AssistenteClienteComponent } from '../../../shared/components/assistente-cliente/assistente-cliente.component';

export interface GuiaPassoAPasso {
  id: string;
  titulo: string;
  resumo: string;
  duracao: string;
  passos: string[];
  duvidaSugerida: string;
}

export type SeloTutorial = 'Novo' | 'Em andamento' | 'Concluído';

interface ProgressoGuia {
  passo: number;
  concluido: boolean;
}

const CHAVE_PROGRESSO = 'gamb.tutoriais.progresso';

@Component({
  selector: 'app-tutoriais',
  standalone: true,
  imports: [CommonModule, RouterLink, AssistenteClienteComponent],
  templateUrl: './tutoriais.component.html'
})
export class TutoriaisComponent implements OnInit {
  private readonly avisoService = inject(AvisoService);

  @ViewChild(AssistenteClienteComponent) assistenteRef?: AssistenteClienteComponent;

  readonly carregandoAvisos = signal(true);
  readonly tutoriaisSistema = signal<AvisoDTO[]>([]);
  readonly guiaAberto = signal<string | null>(null);
  readonly progresso = signal<Record<string, ProgressoGuia>>({});

  readonly guias: GuiaPassoAPasso[] = [
    {
      id: 'whatsapp',
      titulo: 'Conectar o WhatsApp',
      resumo: 'Escaneie o QR Code e mantenha a sessão conectada.',
      duracao: '3 min',
      passos: [
        'Acesse a página inicial do Painel do Cliente.',
        'Localize o cartão "Conexão WhatsApp" e clique em "Iniciar Conexão".',
        'No celular, abra o WhatsApp e vá em "Configurações" > "Aparelhos Conectados" > "Conectar um Aparelho".',
        'Aponte a câmera para o QR Code gerado na tela.',
        'Quando o status mudar para "Conectado" em verde, o bot já responde automaticamente.'
      ],
      duvidaSugerida: 'Como conectar o WhatsApp via QR Code passo a passo?'
    },
    {
      id: 'horarios',
      titulo: 'Horários e locação',
      resumo: 'Defina se o espaço trabalha por diária ou por hora.',
      duracao: '4 min',
      passos: [
        'No menu lateral, clique em "Configuração do chatbot".',
        'Na aba "Geral & Locação", ative ou desative "Locação por hora".',
        'Selecione a duração de cada atendimento e o intervalo para preparo.',
        'Preencha a mensagem de saudação do primeiro contato.',
        'Clique em "Salvar Alterações" no rodapé do formulário.'
      ],
      duvidaSugerida: 'Como configurar locação por hora e por diária?'
    },
    {
      id: 'agenda',
      titulo: 'Google Agenda',
      resumo: 'Sincronize as reservas direto na sua conta Google.',
      duracao: '5 min',
      passos: [
        'No menu de Chatbot, acesse a aba "Agenda Google".',
        'Insira o ID da sua Google Agenda.',
        'Confirme que a conta de serviço do Tech-SaaS tem permissão de leitura e gravação na agenda.',
        'Ative "Permitir Reserva Automática" se quiser confirmação sem intervenção manual.',
        'As reservas confirmadas no bot passam a aparecer como eventos no calendário.'
      ],
      duvidaSugerida: 'Como sincronizar as reservas com a minha Google Agenda?'
    },
    {
      id: 'regras-fotos',
      titulo: 'Fotos, preços e regras',
      resumo: 'Cadastre fotos, tabela de valores e termos do local.',
      duracao: '4 min',
      passos: [
        'Acesse "Configuração do chatbot" e role até Tabela de Preços e Regras de Locação.',
        'Escreva os valores de forma limpa, um por linha.',
        'Cole o link da foto principal e os links da galeria.',
        'Adicione o link do Google Maps para o bot enviar a rota de chegada.',
        'Salve as alterações para o bot passar essas informações aos clientes.'
      ],
      duvidaSugerida: 'Como cadastrar fotos, link do Google Maps e tabela de preços?'
    }
  ];

  ngOnInit(): void {
    this.progresso.set(this.lerProgresso());
    this.carregarAvisosTutoriais();
  }

  carregarAvisosTutoriais(): void {
    this.carregandoAvisos.set(true);
    this.avisoService.listarAtivos().subscribe({
      next: (avisos) => {
        const tutoriais = (avisos ?? []).filter((a) => a.tipo === 'TUTORIAL');
        this.tutoriaisSistema.set(tutoriais);
        this.carregandoAvisos.set(false);
      },
      error: () => {
        this.tutoriaisSistema.set([]);
        this.carregandoAvisos.set(false);
      }
    });
  }

  get guiaSelecionado(): GuiaPassoAPasso | null {
    const id = this.guiaAberto();
    return this.guias.find((g) => g.id === id) ?? null;
  }

  get passoAtual(): number {
    const guia = this.guiaSelecionado;
    return guia ? Math.min(this.progresso()[guia.id]?.passo ?? 0, guia.passos.length - 1) : 0;
  }

  get percentualProgresso(): number {
    const guia = this.guiaSelecionado;
    return guia ? Math.round(((this.passoAtual + 1) / guia.passos.length) * 100) : 0;
  }

  selo(id: string): SeloTutorial {
    const item = this.progresso()[id];
    if (item?.concluido) {
      return 'Concluído';
    }
    return item ? 'Em andamento' : 'Novo';
  }

  abrir(id: string): void {
    this.guiaAberto.set(id);
    if (!this.progresso()[id]) {
      this.gravarProgresso(id, { passo: 0, concluido: false });
    }
  }

  voltar(): void {
    this.guiaAberto.set(null);
  }

  anterior(): void {
    const guia = this.guiaSelecionado;
    if (guia && this.passoAtual > 0) {
      this.gravarProgresso(guia.id, { passo: this.passoAtual - 1, concluido: false });
    }
  }

  proximo(): void {
    const guia = this.guiaSelecionado;
    if (!guia) {
      return;
    }
    if (this.passoAtual >= guia.passos.length - 1) {
      this.gravarProgresso(guia.id, { passo: guia.passos.length - 1, concluido: true });
      this.guiaAberto.set(null);
      return;
    }
    this.gravarProgresso(guia.id, { passo: this.passoAtual + 1, concluido: false });
  }

  tirarDuvidaNoAssistente(duvida: string): void {
    if (this.assistenteRef) {
      this.assistenteRef.usarSugestao(duvida);
      document.getElementById('chat')?.scrollIntoView({ behavior: 'smooth' });
    }
  }

  perguntarSobrePasso(): void {
    const guia = this.guiaSelecionado;
    if (!guia) {
      return;
    }
    const passo = guia.passos[this.passoAtual];
    this.tirarDuvidaNoAssistente(`No tutorial "${guia.titulo}", passo ${this.passoAtual + 1}: ${passo} Pode me ajudar com isso?`);
  }

  private gravarProgresso(id: string, item: ProgressoGuia): void {
    const atualizado = { ...this.progresso(), [id]: item };
    this.progresso.set(atualizado);
    try {
      localStorage.setItem(CHAVE_PROGRESSO, JSON.stringify(atualizado));
    } catch {
      return;
    }
  }

  private lerProgresso(): Record<string, ProgressoGuia> {
    try {
      const bruto = localStorage.getItem(CHAVE_PROGRESSO);
      return bruto ? (JSON.parse(bruto) as Record<string, ProgressoGuia>) : {};
    } catch {
      return {};
    }
  }
}
