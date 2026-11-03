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
  passos: string[];
  icone: string;
  duvidaSugerida: string;
}

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
  readonly cardExpandido = signal<string | null>('whatsapp');

  readonly guias: GuiaPassoAPasso[] = [
    {
      id: 'whatsapp',
      titulo: '1. Conectar WhatsApp via QR Code',
      resumo: 'Aprenda como escanear o QR Code e manter a sessão conectada para atendimento automatizado.',
      passos: [
        'Acesse a página inicial do Painel do Cliente ou o menu de Chatbot.',
        'Localize o cartão "Conexão WhatsApp" e clique no botão "Iniciar Conexão".',
        'Abra o WhatsApp no seu smartphone, vá em "Configurações" > "Aparelhos Conectados" > "Conectar um Aparelho".',
        'Aponte a câmera para o QR Code gerado na tela.',
        'Quando o status mudar para "Conectado" em verde, seu bot já começará a responder automaticamente.'
      ],
      icone: 'qr',
      duvidaSugerida: 'Como conectar o WhatsApp via QR Code passo a passo?'
    },
    {
      id: 'horarios',
      titulo: '2. Configurar Horários e Modalidade de Locação',
      resumo: 'Defina se seu espaço trabalha por diária fechada ou por hora de atendimento.',
      passos: [
        'No menu lateral, clique em "Configuração do chatbot".',
        'Na aba "Geral & Locação", ative ou desative "Locação por hora".',
        'Selecione a duração de cada atendimento e o tempo de intervalo para higienização/preparo.',
        'Preencha a mensagem de saudação que o cliente receberá no primeiro contato.',
        'Clique em "Salvar Alterações" no rodapé do formulário.'
      ],
      icone: 'relogio',
      duvidaSugerida: 'Como configurar modalidade de locação por hora vs diária?'
    },
    {
      id: 'agenda',
      titulo: '3. Sincronização com Google Agenda',
      resumo: 'Evite conflitos de reservas sincronizando os agendamentos diretamente na sua conta Google.',
      passos: [
        'No menu de Chatbot, acesse a aba "Agenda Google".',
        'Insira o ID da sua Google Agenda (ex: usuario@gmail.com ou o identificador do calendário).',
        'Verifique se a conta de serviço do Tech-SaaS possui permissão de leitura/gravação em sua agenda.',
        'Ative a opção "Permitir Reserva Automática" se desejar confirmação sem intervenção manual.',
        'As novas reservas confirmadas no bot serão lançadas instantaneamente como eventos no seu calendário.'
      ],
      icone: 'calendario',
      duvidaSugerida: 'Como sincronizar as reservas com a minha Google Agenda?'
    },
    {
      id: 'regras-fotos',
      titulo: '4. Fotos, Preços e Regras de Locação',
      resumo: 'Disponibilize fotos do espaço, tabela de valores e termos de uso do local.',
      passos: [
        'Acesse "Configuração do chatbot" e role até os campos de Tabela de Preços e Regras de Locação.',
        'Escreva os valores de forma limpa (ex: Segunda a Quinta R$ 500,00 | Fins de semana R$ 900,00).',
        'Cole o link da foto principal de divulgação e os links da galeria de fotos.',
        'Adicione o link do Google Maps para que o bot forneça rota de chegada.',
        'Salve as alterações para que o bot envie essas informações aos clientes interessados.'
      ],
      icone: 'fotos',
      duvidaSugerida: 'Como cadastrar fotos, link do Google Maps e tabela de preços?'
    },
    {
      id: 'simulador',
      titulo: '5. Sessão Virtual de Testes (Sem QR Code)',
      resumo: 'Experimente as respostas e fluxo do bot em memória sem precisar escanear QR Code.',
      passos: [
        'Role esta tela até a caixa do "Assistente Inteligente & Simulador" logo abaixo.',
        'Alterne para a aba "Simulador Virtual".',
        'Envie "oi", "menu", "preço" ou "regras" para ver a resposta que o cliente receberia.',
        'Valide se as opções de menu e textos cadastrados estão exatamente como você deseja.',
        'Esse teste funciona totalmente em memória e não consome recursos de conexão física.'
      ],
      icone: 'simulador',
      duvidaSugerida: 'Como funciona a sessão virtual de testes de novos módulos?'
    }
  ];

  ngOnInit(): void {
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

  alternarCard(id: string): void {
    this.cardExpandido.update((atual) => (atual === id ? null : id));
  }

  tirarDuvidaNoAssistente(duvida: string): void {
    if (this.assistenteRef) {
      this.assistenteRef.usarSugestao(duvida);
      const elemento = document.getElementById('secao-assistente-cliente');
      if (elemento) {
        elemento.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }

  abrirSimuladorVirtual(): void {
    if (this.assistenteRef) {
      this.assistenteRef.alternarModo('TESTE_VIRTUAL');
      const elemento = document.getElementById('secao-assistente-cliente');
      if (elemento) {
        elemento.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }
}
