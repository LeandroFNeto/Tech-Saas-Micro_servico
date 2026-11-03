import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { PrevisaoService } from '../../../core/services/previsao.service';
import { ConexaoWhatsappComponent } from '../../../shared/components/conexao-whatsapp/conexao-whatsapp.component';
import { EmpresaView } from '../../../shared/models/empresa.models';
import { PrevisaoDia, PrevisaoTempoResponse, RiscoDesmarcacao } from '../../../shared/models/previsao.models';
import contentData from '../../../../../../especificacao/front/cliente-painel.content.json';
import designData from '../../../../../../especificacao/front/telas.design.json';

type Tempo = 'sol' | 'nuvem' | 'chuva' | 'temporal';

interface DiaExibicao extends PrevisaoDia {
  dataCurta: string;
  semanaCurta: string;
  tempo: Tempo;
}

function tempoPorCodigo(codigo: number): Tempo {
  if (codigo >= 200 && codigo < 300) {
    return 'temporal';
  }
  if (codigo >= 300 && codigo < 600) {
    return 'chuva';
  }
  if (codigo === 800) {
    return 'sol';
  }
  return 'nuvem';
}

function dataCurta(data: string): string {
  const [, mes, dia] = data.split('-');
  return mes && dia ? `${dia}/${mes}` : data;
}

function semanaCurta(diaSemana: string): string {
  const texto = (diaSemana ?? '').trim().slice(0, 3);
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

@Component({
  selector: 'app-painel',
  imports: [RouterLink, ConexaoWhatsappComponent],
  templateUrl: './painel.component.html'
})
export class PainelComponent {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  private readonly previsaoService = inject(PrevisaoService);

  readonly conteudo = contentData.clientePainel;
  readonly design = designData.telas.clientePainel;

  readonly sessao = this.auth.sessao()?.sessaoWhatsapp ?? '';

  readonly carregando = signal(true);
  readonly falha = signal(false);
  readonly cidadeSalva = signal('');
  readonly ativaSalva = signal(false);
  readonly cidadeResolvida = signal('');
  readonly previsao = signal<PrevisaoTempoResponse | null>(null);

  readonly editando = signal(false);
  readonly cidade = signal('');
  readonly previsaoAtiva = signal(false);
  readonly permitePrevisaoTempo = signal(true);
  readonly salvando = signal(false);
  readonly erroCidade = signal('');
  readonly sucesso = signal(false);

  readonly semCidade = computed(() => !this.carregando() && !this.falha() && this.cidadeSalva().trim() === '');
  readonly formularioAberto = computed(() => this.editando() || this.semCidade());
  readonly desligada = computed(
    () => !this.carregando() && !this.falha() && !this.semCidade() && !this.ativaSalva()
  );
  readonly erro = computed(() => {
    if (this.carregando() || this.semCidade() || this.desligada()) {
      return false;
    }
    return this.falha() || this.previsao()?.disponivel === false;
  });
  readonly reservasIndisponiveis = computed(() => {
    const dados = this.previsao();
    return !!dados && dados.disponivel && !dados.reservasDisponiveis;
  });
  readonly vazio = computed(() => {
    const dados = this.previsao();
    return this.pronta() && !!dados && dados.dias.length === 0;
  });
  readonly mostrarDias = computed(() => {
    const dados = this.previsao();
    return this.pronta() && !!dados && dados.dias.length > 0;
  });
  readonly dias = computed<DiaExibicao[]>(() =>
    (this.previsao()?.dias ?? []).map((dia) => ({
      ...dia,
      dataCurta: dataCurta(dia.data),
      semanaCurta: semanaCurta(dia.diaSemana),
      tempo: tempoPorCodigo(dia.codigoTempo)
    }))
  );
  readonly cidadeExibida = computed(
    () => this.previsao()?.cidade || this.cidadeResolvida() || this.cidadeSalva()
  );
  readonly atualizadoAs = computed(() => {
    const valor = this.previsao()?.atualizadoEm;
    return valor && valor.length >= 16 ? valor.slice(11, 16) : '';
  });
  readonly esqueletos = Array.from({ length: 8 }, (_, indice) => indice);

  constructor() {
    this.carregar();
  }

  private pronta(): boolean {
    return !this.carregando() && !this.erro() && !this.semCidade() && !this.desligada();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.falha.set(false);
    this.empresaService.buscarPorSessao(this.sessao).subscribe({
      next: (empresa) => {
        this.aplicarEmpresa(empresa);
        this.buscarPrevisao();
      },
      error: () => {
        this.falha.set(true);
        this.carregando.set(false);
      }
    });
  }

  private aplicarEmpresa(
    empresa: Pick<EmpresaView, 'cidadePrevisao' | 'cidadePrevisaoResolvida' | 'previsaoTempoAtiva' | 'permitePrevisaoTempo'>
  ): void {
    this.cidadeSalva.set(empresa.cidadePrevisao ?? '');
    this.cidadeResolvida.set(empresa.cidadePrevisaoResolvida ?? '');
    const permitido = empresa.permitePrevisaoTempo !== false;
    this.permitePrevisaoTempo.set(permitido);
    const ativa = permitido && !!empresa.previsaoTempoAtiva;
    this.ativaSalva.set(ativa);
    this.cidade.set(empresa.cidadePrevisao ?? '');
    this.previsaoAtiva.set(ativa);
  }

  private buscarPrevisao(): void {
    if (this.cidadeSalva().trim() === '' || !this.ativaSalva()) {
      this.previsao.set(null);
      this.carregando.set(false);
      return;
    }
    this.carregando.set(true);
    this.previsaoService.buscar(this.sessao).subscribe({
      next: (dados) => {
        this.previsao.set(dados);
        this.falha.set(false);
        this.carregando.set(false);
      },
      error: () => {
        this.previsao.set(null);
        this.falha.set(true);
        this.carregando.set(false);
      }
    });
  }

  atualizarCidade(evento: Event): void {
    this.cidade.set((evento.target as HTMLInputElement).value);
  }

  alternarPrevisao(): void {
    if (!this.permitePrevisaoTempo()) {
      return;
    }
    this.previsaoAtiva.set(!this.previsaoAtiva());
  }

  abrirEdicao(): void {
    this.cidade.set(this.cidadeSalva());
    this.previsaoAtiva.set(this.ativaSalva());
    this.erroCidade.set('');
    this.sucesso.set(false);
    this.editando.set(true);
  }

  cancelarEdicao(): void {
    this.editando.set(false);
    this.erroCidade.set('');
    this.cidade.set(this.cidadeSalva());
    this.previsaoAtiva.set(this.ativaSalva());
  }

  salvarCidade(): void {
    if (this.salvando()) {
      return;
    }
    this.salvando.set(true);
    this.erroCidade.set('');
    this.sucesso.set(false);
    this.empresaService
      .atualizarPeloCliente(this.sessao, {
        cidadePrevisao: this.cidade().trim(),
        previsaoTempoAtiva: this.previsaoAtiva()
      })
      .subscribe({
        next: (empresa) => {
          this.salvando.set(false);
          this.editando.set(false);
          this.aplicarEmpresa(empresa);
          this.sucesso.set(true);
          this.buscarPrevisao();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erroCidade.set(this.mensagemErroCidade(erro));
        }
      });
  }

  private mensagemErroCidade(erro: unknown): string {
    const estados = this.conteudo.previsao.estados;
    const padrao: Record<number, string> = {
      400: estados.cidadeNaoEncontrada,
      429: estados.limiteTrocas,
      503: estados.provedorIndisponivel
    };
    if (erro instanceof HttpErrorResponse && padrao[erro.status]) {
      const doBack = erro.error?.message;
      return typeof doBack === 'string' && doBack.trim() !== '' ? doBack : padrao[erro.status];
    }
    return estados.provedorIndisponivel;
  }

  classeCartao(reservado: boolean): string {
    return reservado ? this.design.dias.cartaoReservado : this.design.dias.cartao;
  }

  classeRisco(risco: RiscoDesmarcacao, reservado: boolean): string {
    const cores = this.design.risco[risco];
    return `${this.design.risco.base} ${reservado ? cores.forte : cores.suave}`;
  }

  rotuloRisco(risco: RiscoDesmarcacao): string {
    return this.conteudo.previsao.risco[risco];
  }

  formatarMm(valor: number): string {
    return valor.toFixed(1).replace('.', ',');
  }
}
