import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AutomacaoService, ContatoResumoDTO, ReservaResumoDTO } from '../../../core/services/automacao.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import contentData from '../../../../../../especificacao/front/cliente-automacao.content.json';

type AbaAtiva = 'lembrete' | 'reservas' | 'contatos';

@Component({
  selector: 'app-automacao',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './automacao.component.html'
})
export class AutomacaoComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly automacao = inject(AutomacaoService);
  private readonly empresaService = inject(EmpresaService);

  readonly textos = contentData.clienteAutomacao;
  readonly abaAtiva = signal<AbaAtiva>('lembrete');

  readonly lembreteModo = signal<'DESLIGADO' | 'SUGERIDO' | 'AUTOMATICO'>('DESLIGADO');
  readonly lembreteDiasAntes = signal<number>(1);
  readonly lembreteHorarioEnvio = signal<string>('10:00');
  readonly lembreteTexto = signal<string>('');
  readonly lembreteSalvando = signal<boolean>(false);
  readonly lembreteMensagemSucesso = signal<string | null>(null);
  readonly lembreteMensagemErro = signal<string | null>(null);

  readonly reservas = signal<ReservaResumoDTO[]>([]);
  readonly carregandoReservas = signal<boolean>(false);
  readonly erroReservas = signal<string | null>(null);
  readonly enviandoLembreteId = signal<number | null>(null);

  readonly modalContatoAberto = signal<boolean>(false);
  readonly reservaParaVincular = signal<ReservaResumoDTO | null>(null);
  readonly contatoIdSelecionado = signal<number | null>(null);
  readonly novoTelefone = signal<string>('');
  readonly vinculandoContato = signal<boolean>(false);

  readonly contatos = signal<ContatoResumoDTO[]>([]);
  readonly carregandoContatos = signal<boolean>(false);
  readonly numerosRevelados = signal<Record<number, string>>({});
  readonly revelandoContatoId = signal<number | null>(null);
  readonly avisoLimite = signal<string | null>(null);

  ngOnInit(): void {
    this.carregarDadosEmpresa();
  }

  sessao(): string {
    return this.auth.sessao()?.sessaoWhatsapp ?? '';
  }

  mudarAba(aba: AbaAtiva): void {
    this.abaAtiva.set(aba);
    if (aba === 'reservas') {
      this.carregarReservas();
    } else if (aba === 'contatos') {
      this.carregarContatos();
    }
  }

  carregarDadosEmpresa(): void {
    const s = this.sessao();
    if (!s) return;
    this.empresaService.buscarPorSessao(s).subscribe({
      next: (emp) => {
        if (emp.lembrete) {
          this.lembreteModo.set(emp.lembrete.modo ?? 'DESLIGADO');
          this.lembreteDiasAntes.set(emp.lembrete.diasAntes ?? 1);
          this.lembreteHorarioEnvio.set(emp.lembrete.horarioEnvio ?? '10:00');
          this.lembreteTexto.set(emp.lembrete.texto ?? '');
        }
      }
    });
  }

  salvarLembrete(): void {
    const s = this.sessao();
    if (!s) return;
    this.lembreteSalvando.set(true);
    this.lembreteMensagemSucesso.set(null);
    this.lembreteMensagemErro.set(null);

    this.empresaService.atualizarPeloCliente(s, {
      lembrete: {
        modo: this.lembreteModo(),
        diasAntes: Number(this.lembreteDiasAntes()),
        horarioEnvio: this.lembreteHorarioEnvio(),
        texto: this.lembreteTexto()
      }
    }).subscribe({
      next: () => {
        this.lembreteSalvando.set(false);
        this.lembreteMensagemSucesso.set(this.textos.lembrete.botoes.sucesso);
      },
      error: () => {
        this.lembreteSalvando.set(false);
        this.lembreteMensagemErro.set(this.textos.lembrete.botoes.erro);
      }
    });
  }

  carregarReservas(): void {
    const s = this.sessao();
    if (!s) return;
    this.carregandoReservas.set(true);
    this.erroReservas.set(null);
    this.automacao.listarReservas(s).subscribe({
      next: (dados) => {
        this.reservas.set(dados);
        this.carregandoReservas.set(false);
      },
      error: () => {
        this.erroReservas.set(this.textos.reservas.tabela.erro);
        this.carregandoReservas.set(false);
      }
    });
  }

  avisarAgora(reserva: ReservaResumoDTO): void {
    const s = this.sessao();
    if (!s) return;
    this.enviandoLembreteId.set(reserva.id);
    this.automacao.enviarLembrete(s, reserva.id).subscribe({
      next: () => {
        this.enviandoLembreteId.set(null);
        this.carregarReservas();
      },
      error: () => {
        this.enviandoLembreteId.set(null);
      }
    });
  }

  abrirModalContato(reserva: ReservaResumoDTO): void {
    this.reservaParaVincular.set(reserva);
    this.contatoIdSelecionado.set(null);
    this.novoTelefone.set('');
    this.modalContatoAberto.set(true);
    if (this.contatos().length === 0) {
      this.carregarContatos();
    }
  }

  fecharModalContato(): void {
    this.modalContatoAberto.set(false);
    this.reservaParaVincular.set(null);
  }

  confirmarVinculoContato(): void {
    const s = this.sessao();
    const r = this.reservaParaVincular();
    if (!s || !r) return;

    this.vinculandoContato.set(true);
    this.automacao.vincularContato(s, r.id, {
      contatoId: this.contatoIdSelecionado() ?? undefined,
      numeroTelefone: this.novoTelefone().trim() || undefined
    }).subscribe({
      next: () => {
        this.vinculandoContato.set(false);
        this.fecharModalContato();
        this.carregarReservas();
      },
      error: () => {
        this.vinculandoContato.set(false);
      }
    });
  }

  carregarContatos(): void {
    const s = this.sessao();
    if (!s) return;
    this.carregandoContatos.set(true);
    this.automacao.listarContatos(s).subscribe({
      next: (dados) => {
        this.contatos.set(dados);
        this.carregandoContatos.set(false);
      },
      error: () => {
        this.carregandoContatos.set(false);
      }
    });
  }

  revelarNumero(contatoId: number): void {
    const s = this.sessao();
    if (!s) return;
    this.revelandoContatoId.set(contatoId);
    this.avisoLimite.set(null);

    this.automacao.obterNumeroContato(s, contatoId).subscribe({
      next: (res) => {
        this.numerosRevelados.update((ant) => ({ ...ant, [contatoId]: res.numero }));
        this.revelandoContatoId.set(null);
      },
      error: (err) => {
        this.revelandoContatoId.set(null);
        if (err.status === 429) {
          this.avisoLimite.set(this.textos.contatos.avisoLimite);
        }
      }
    });
  }

  obterTelefoneExibicao(contato: ContatoResumoDTO): string {
    return this.numerosRevelados()[contato.id] || contato.telefoneMascara;
  }

  descricaoSituacao(chave: string): string {
    const sit = this.textos.reservas.situacoes as Record<string, string>;
    return sit[chave] ?? chave;
  }
}
