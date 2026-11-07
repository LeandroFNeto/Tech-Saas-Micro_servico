import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { AutomacaoService, ContatoResumoDTO, ReservaResumoDTO } from '../../../core/services/automacao.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import contentData from '../../../../../../especificacao/front/cliente-automacao.content.json';

type AbaAtiva = 'lembrete' | 'reservas' | 'contatos';

const PADRAO_HORARIO = /^([01]\d|2[0-3]):[0-5]\d$/;

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
  readonly erroModalContato = signal<string | null>(null);

  readonly contatos = signal<ContatoResumoDTO[]>([]);
  readonly carregandoContatos = signal<boolean>(false);
  readonly erroContatos = signal<string | null>(null);
  readonly numerosRevelados = signal<Record<number, string>>({});
  readonly revelandoContatoId = signal<number | null>(null);
  readonly avisoLimite = signal<string | null>(null);

  ngOnInit(): void {
    this.carregarDadosEmpresa();
  }

  @HostListener('document:keydown.escape')
  aoPressionarEscape(): void {
    if (this.modalContatoAberto()) {
      this.fecharModalContato();
    }
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
    this.lembreteMensagemErro.set(null);
    this.empresaService.buscarPorSessao(s).subscribe({
      next: (emp) => {
        const cfg = emp.lembreteConfirmacao;
        if (cfg) {
          this.lembreteModo.set(cfg.modo ?? 'DESLIGADO');
          this.lembreteDiasAntes.set(cfg.diasAntes ?? 1);
          this.lembreteHorarioEnvio.set(cfg.horarioEnvio ?? '10:00');
          this.lembreteTexto.set(cfg.texto ?? '');
        }
      },
      error: (err: unknown) => {
        this.lembreteMensagemErro.set(mensagemHttp(err));
      }
    });
  }

  salvarLembrete(): void {
    const s = this.sessao();
    if (!s) return;
    this.lembreteMensagemSucesso.set(null);
    this.lembreteMensagemErro.set(null);

    if (this.lembreteModo() !== 'DESLIGADO') {
      if (!PADRAO_HORARIO.test(this.lembreteHorarioEnvio())) {
        this.lembreteMensagemErro.set('Informe um horário de envio válido no formato HH:mm.');
        return;
      }
      if (this.lembreteTexto().length > 1000) {
        this.lembreteMensagemErro.set('O texto do lembrete deve ter no máximo 1000 caracteres.');
        return;
      }
    }

    this.lembreteSalvando.set(true);
    this.empresaService.atualizarPeloCliente(s, {
      lembreteConfirmacao: {
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
    this.erroReservas.set(null);
    this.enviandoLembreteId.set(reserva.id);
    this.automacao.enviarLembrete(s, reserva.id).subscribe({
      next: () => {
        this.enviandoLembreteId.set(null);
        this.carregarReservas();
      },
      error: (err: unknown) => {
        this.enviandoLembreteId.set(null);
        this.erroReservas.set(mensagemHttp(err));
      }
    });
  }

  abrirModalContato(reserva: ReservaResumoDTO): void {
    this.reservaParaVincular.set(reserva);
    this.contatoIdSelecionado.set(null);
    this.novoTelefone.set('');
    this.erroModalContato.set(null);
    this.modalContatoAberto.set(true);
    if (this.contatos().length === 0) {
      this.carregarContatos();
    }
  }

  fecharModalContato(): void {
    this.modalContatoAberto.set(false);
    this.reservaParaVincular.set(null);
    this.erroModalContato.set(null);
  }

  confirmarVinculoContato(): void {
    const s = this.sessao();
    const r = this.reservaParaVincular();
    if (!s || !r) return;

    this.erroModalContato.set(null);
    const contatoId = this.contatoIdSelecionado() ?? undefined;
    const telefoneTexto = this.novoTelefone().trim();
    const digitos = telefoneTexto.replace(/\D/g, '');

    if (!contatoId && !telefoneTexto) {
      this.erroModalContato.set('Selecione um contato existente ou informe um telefone válido.');
      return;
    }
    if (telefoneTexto && (digitos.length < 10 || digitos.length > 13)) {
      this.erroModalContato.set('Informe um número de telefone válido com DDD.');
      return;
    }

    this.vinculandoContato.set(true);
    this.automacao.vincularContato(s, r.id, {
      contatoId,
      telefone: telefoneTexto || undefined
    }).subscribe({
      next: () => {
        this.vinculandoContato.set(false);
        this.fecharModalContato();
        this.carregarReservas();
      },
      error: (err: unknown) => {
        this.vinculandoContato.set(false);
        this.erroModalContato.set(mensagemHttp(err));
      }
    });
  }

  carregarContatos(): void {
    const s = this.sessao();
    if (!s) return;
    this.carregandoContatos.set(true);
    this.erroContatos.set(null);
    this.automacao.listarContatos(s).subscribe({
      next: (dados) => {
        this.contatos.set(dados);
        this.carregandoContatos.set(false);
      },
      error: (err: unknown) => {
        this.carregandoContatos.set(false);
        this.erroContatos.set(mensagemHttp(err));
      }
    });
  }

  revelarNumero(contatoId: number): void {
    const s = this.sessao();
    if (!s) return;
    this.revelandoContatoId.set(contatoId);
    this.avisoLimite.set(null);
    this.erroContatos.set(null);

    this.automacao.obterNumeroContato(s, contatoId).subscribe({
      next: (res) => {
        this.numerosRevelados.update((ant) => ({ ...ant, [contatoId]: res.numero }));
        this.revelandoContatoId.set(null);
      },
      error: (err: { status?: number }) => {
        this.revelandoContatoId.set(null);
        if (err?.status === 429) {
          this.avisoLimite.set(this.textos.contatos.avisoLimite);
        } else {
          this.erroContatos.set(mensagemHttp(err));
        }
      }
    });
  }

  obterTelefoneExibicao(contato: ContatoResumoDTO): string {
    return this.numerosRevelados()[contato.id] || contato.telefoneMascarado;
  }

  descricaoSituacao(chave: string): string {
    const sit = this.textos.reservas.situacoes as Record<string, string>;
    return sit[chave] ?? chave;
  }
}
