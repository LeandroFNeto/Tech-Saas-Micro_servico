import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { FuncionamentoService } from '../../../core/services/funcionamento.service';
import {
  DataEspecialCreateDTO,
  DataEspecialDTO,
  FuncionamentoDTO,
  FuncionamentoUpdateDTO,
  HorarioFuncionamentoDTO
} from '../../../shared/models/funcionamento.models';
import contentData from '../../../../../../especificacao/front/cliente-perfil.content.json';
import designData from '../../../../../../especificacao/front/telas.design.json';

export type EstadoFuncionamento = 'normal' | 'carregando' | 'vazio' | 'erro' | 'sucesso';

export type CodigoDia = HorarioFuncionamentoDTO['perfil'];

export interface HorarioDia {
  codigo: CodigoDia;
  aberto: boolean;
  abre: string;
  fecha: string;
  duracao: number | null;
}

export interface DiaEspecial {
  id: number;
  data: string;
  fechado: boolean;
  abre: string;
  fecha: string;
  motivo: string;
}

export interface AvisoReservas {
  data: string;
  eventos: string[];
}

export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
] as const;

export const DURACOES = Array.from({ length: 24 }, (_, i) => (i + 1) * 30);
export const DIAS_A_FRENTE = 180;
export const MAX_MOTIVO = 120;
export const MAX_INTERVALO = 120;

export const CODIGOS_DIA: CodigoDia[] = ['SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO', 'DOMINGO', 'FERIADO'];
const ABRE_PADRAO = '08:00';
const FECHA_PADRAO = '22:00';
const DURACAO_PADRAO = 60;

function hora(valor: string | null | undefined): string {
  return valor ? valor.slice(0, 5) : '';
}

function mensagemDoServidor(erro: unknown): string | null {
  if (!(erro instanceof HttpErrorResponse) || erro.status !== 400) {
    return null;
  }
  const corpo = erro.error as unknown;
  if (typeof corpo === 'string') {
    return corpo.trim() || null;
  }
  if (corpo && typeof corpo === 'object') {
    const registro = corpo as Record<string, unknown>;
    for (const chave of ['message', 'mensagem', 'erro', 'error', 'detail']) {
      const valor = registro[chave];
      if (typeof valor === 'string' && valor.trim()) {
        return valor.trim();
      }
    }
  }
  return null;
}

function paraIso(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

function deIso(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

@Component({
  selector: 'app-funcionamento',
  templateUrl: './funcionamento.component.html'
})
export class FuncionamentoComponent {
  readonly conteudo = contentData.clientePerfil.funcionamento;
  readonly design = designData.telas.clientePerfil;
  readonly visual = designData.telas.clientePerfil.funcionamento;
  readonly ufs = UFS;
  readonly duracoes = DURACOES;
  readonly maxMotivo = MAX_MOTIVO;
  readonly maxIntervalo = MAX_INTERVALO;
  readonly dataMinima: string;
  readonly dataMaxima: string;

  private readonly api = inject(FuncionamentoService);
  private readonly auth = inject(AuthService);
  readonly sessao = this.auth.sessao()?.sessaoWhatsapp ?? '';

  readonly estado = signal<EstadoFuncionamento>('carregando');
  readonly salvando = signal(false);
  readonly mensagemErro = signal<string | null>(null);
  readonly erroAcao = signal<{ titulo: string; texto: string } | null>(null);
  readonly personalizarAberto = signal(false);
  readonly abre = signal(ABRE_PADRAO);
  readonly fecha = signal(FECHA_PADRAO);
  readonly duracao = signal(DURACAO_PADRAO);
  readonly horarios = signal<HorarioDia[]>([]);
  readonly intervalo = signal<number | null>(0);
  readonly uf = signal('');
  readonly especiais = signal<DiaEspecial[]>([]);
  readonly aviso = signal<AvisoReservas | null>(null);
  readonly novaData = signal('');
  readonly novoMotivo = signal('');
  readonly novoFechado = signal(true);
  readonly novoAbre = signal('');
  readonly novoFecha = signal('');

  readonly carregando = computed(() => this.estado() === 'carregando');
  readonly comErro = computed(() => this.estado() === 'erro');
  readonly pronto = computed(() => !this.carregando() && !this.comErro());
  readonly sucesso = computed(() => this.estado() === 'sucesso');
  readonly vazio = computed(() => this.estado() === 'vazio');

  readonly horarioInvalido = computed(() => !this.fechaDepois(this.abre(), this.fecha()));
  readonly intervaloInvalido = computed(() => {
    const valor = this.intervalo();
    return valor === null || !Number.isInteger(valor) || valor < 0 || valor > MAX_INTERVALO;
  });
  readonly ufInvalida = computed(() => this.uf() !== '' && !(UFS as readonly string[]).includes(this.uf()));
  readonly algumDiaInvalido = computed(() => this.horarios().some((dia) => this.diaInvalido(dia)));
  readonly salvarDesabilitado = computed(
    () =>
      this.salvando() ||
      this.horarioInvalido() ||
      !this.duracaoValida(this.duracao()) ||
      this.algumDiaInvalido() ||
      this.intervaloInvalido() ||
      this.ufInvalida()
  );

  readonly dataInvalida = computed(() => {
    const data = this.novaData();
    return data !== '' && (data < this.dataMinima || data > this.dataMaxima);
  });
  readonly motivoInvalido = computed(() => this.novoMotivo().length > MAX_MOTIVO);
  readonly novoHorarioInvalido = computed(
    () => !this.novoFechado() && !this.fechaDepois(this.novoAbre(), this.novoFecha())
  );
  readonly adicionarDesabilitado = computed(
    () => this.salvando() || this.novaData() === '' || this.dataInvalida() || this.motivoInvalido() || this.novoHorarioInvalido()
  );

  constructor() {
    const hoje = new Date();
    this.dataMinima = paraIso(hoje);
    this.dataMaxima = paraIso(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + DIAS_A_FRENTE));
    this.carregar();
  }

  carregar(): void {
    this.estado.set('carregando');
    this.mensagemErro.set(null);
    this.erroAcao.set(null);
    this.aviso.set(null);
    if (!this.sessao) {
      this.estado.set('erro');
      return;
    }
    this.api.ler(this.sessao).subscribe({
      next: (dto) => this.aplicar(dto),
      error: (erro) => {
        this.mensagemErro.set(mensagemDoServidor(erro));
        this.estado.set('erro');
      }
    });
  }

  alternarPersonalizar(): void {
    this.personalizarAberto.update((aberto) => !aberto);
  }

  valor(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }

  marcado(evento: Event): boolean {
    return (evento.target as HTMLInputElement).checked;
  }

  definirIntervalo(texto: string): void {
    this.intervalo.set(texto.trim() === '' ? null : Number(texto));
  }

  atualizarDia(codigo: CodigoDia, mudanca: Partial<HorarioDia>): void {
    this.horarios.update((lista) => lista.map((dia) => (dia.codigo === codigo ? { ...dia, ...mudanca } : dia)));
  }

  duracaoDoTexto(texto: string): number | null {
    return texto === '' ? null : Number(texto);
  }

  diaInvalido(dia: HorarioDia): boolean {
    if (!dia.aberto) {
      return false;
    }
    return !this.fechaDepois(dia.abre, dia.fecha) || (dia.duracao !== null && !this.duracaoValida(dia.duracao));
  }

  duracaoValida(minutos: number): boolean {
    return Number.isInteger(minutos) && minutos >= 30 && minutos <= 720 && minutos % 30 === 0;
  }

  nomeDia(codigo: CodigoDia): string {
    return this.conteudo.dias[codigo];
  }

  rotuloDuracao(minutos: number): string {
    const u = this.conteudo.unidades;
    if (minutos < 60) {
      return `${minutos} ${u.minutos}`;
    }
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    return resto ? `${horas} ${u.horas} ${resto} ${u.minutos}` : `${horas} ${u.horas}`;
  }

  faixa(abre: string, fecha: string): string {
    return `${abre} ${this.conteudo.unidades.ligacao} ${fecha}`;
  }

  statusDia(dia: HorarioDia): string {
    if (!dia.aberto) {
      return this.conteudo.porDia.fechado;
    }
    const partes = [this.conteudo.porDia.aberto, this.faixa(dia.abre, dia.fecha)];
    if (dia.duracao !== null) {
      partes.push(this.rotuloDuracao(dia.duracao));
    }
    return partes.join(` ${this.conteudo.unidades.separador} `);
  }

  classesDia(dia: HorarioDia): string {
    return dia.aberto ? this.visual.dia.linha : `${this.visual.dia.linha} ${this.visual.dia.linhaFechada}`;
  }

  classesEntrada(base: string, invalida: boolean): string {
    return invalida ? `${base} ${this.visual.entradaInvalida}` : base;
  }

  dataLegivel(iso: string): string {
    return new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' }).format(
      deIso(iso)
    );
  }

  dataCurta(iso: string): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(deIso(iso));
  }

  seloEspecial(dia: DiaEspecial): string {
    return dia.fechado ? this.conteudo.especiais.fechado : this.faixa(dia.abre, dia.fecha);
  }

  classesSelo(dia: DiaEspecial): string {
    const selo = this.visual.especial;
    return `${selo.seloBase} ${dia.fechado ? selo.seloFechado : selo.seloHorario}`;
  }

  adicionarEspecial(): void {
    if (this.adicionarDesabilitado()) {
      return;
    }
    const fechado = this.novoFechado();
    const motivo = this.novoMotivo().trim();
    const dto: DataEspecialCreateDTO = {
      data: this.novaData(),
      fechado,
      ...(fechado ? {} : { abre: this.novoAbre(), fecha: this.novoFecha() }),
      ...(motivo ? { motivo } : {})
    };
    this.salvando.set(true);
    this.erroAcao.set(null);
    this.api.cadastrarDiaEspecial(this.sessao, dto).subscribe({
      next: (criado) => {
        this.salvando.set(false);
        this.especiais.update((lista) =>
          [...lista, this.paraDiaEspecial(criado)].sort((a, b) => a.data.localeCompare(b.data))
        );
        const eventos = criado.reservasNaData ?? [];
        this.aviso.set(eventos.length > 0 ? { data: dto.data, eventos } : null);
        this.limparNovo();
      },
      error: (erro) => this.falhaDeAcao(this.conteudo.erro.diaTitulo, erro)
    });
  }

  removerEspecial(id: number): void {
    if (this.salvando()) {
      return;
    }
    this.salvando.set(true);
    this.erroAcao.set(null);
    this.api.removerDiaEspecial(this.sessao, id).subscribe({
      next: () => {
        this.salvando.set(false);
        this.especiais.update((lista) => lista.filter((dia) => dia.id !== id));
      },
      error: (erro) => this.falhaDeAcao(this.conteudo.erro.diaTitulo, erro)
    });
  }

  salvar(evento: Event): void {
    evento.preventDefault();
    if (this.salvarDesabilitado()) {
      return;
    }
    const dto: FuncionamentoUpdateDTO = {
      duracaoAtendimentoMinutos: this.duracao(),
      intervaloEntreAtendimentosMinutos: this.intervalo() ?? 0,
      uf: this.uf(),
      horarios: this.horariosParaEnvio()
    };
    this.salvando.set(true);
    this.erroAcao.set(null);
    this.api.salvar(this.sessao, dto).subscribe({
      next: (resposta) => {
        this.salvando.set(false);
        this.aplicarHorarios(resposta);
        this.estado.set('sucesso');
      },
      error: (erro) => this.falhaDeAcao(this.conteudo.erro.salvarTitulo, erro)
    });
  }

  private horariosParaEnvio(): HorarioFuncionamentoDTO[] {
    if (this.personalizarAberto()) {
      return this.horarios().map((dia) =>
        dia.aberto
          ? { perfil: dia.codigo, fechado: false, abre: dia.abre, fecha: dia.fecha, duracaoMinutos: dia.duracao }
          : { perfil: dia.codigo, fechado: true, duracaoMinutos: dia.duracao }
      );
    }
    return CODIGOS_DIA.map((codigo) => ({
      perfil: codigo,
      fechado: false,
      abre: this.abre(),
      fecha: this.fecha(),
      duracaoMinutos: null
    }));
  }

  private aplicar(dto: FuncionamentoDTO): void {
    this.aplicarHorarios(dto);
    this.especiais.set(
      (dto.diasEspeciais ?? []).map((dia) => this.paraDiaEspecial(dia)).sort((a, b) => a.data.localeCompare(b.data))
    );
    this.limparNovo();
    this.estado.set((dto.horarios ?? []).length === 0 ? 'vazio' : 'normal');
  }

  private aplicarHorarios(dto: FuncionamentoDTO): void {
    const recebidos = dto.horarios ?? [];
    const lista: HorarioDia[] = CODIGOS_DIA.map((codigo) => {
      const item = recebidos.find((horario) => horario.perfil === codigo);
      if (!item) {
        return { codigo, aberto: true, abre: ABRE_PADRAO, fecha: FECHA_PADRAO, duracao: null };
      }
      return {
        codigo,
        aberto: !item.fechado,
        abre: item.fechado ? '' : hora(item.abre),
        fecha: item.fechado ? '' : hora(item.fecha),
        duracao: item.duracaoMinutos ?? null
      };
    });
    const referencia = lista.find((dia) => dia.aberto);
    this.horarios.set(lista);
    this.abre.set(referencia?.abre || ABRE_PADRAO);
    this.fecha.set(referencia?.fecha || FECHA_PADRAO);
    this.duracao.set(dto.duracaoAtendimentoMinutos ?? DURACAO_PADRAO);
    this.intervalo.set(dto.intervaloEntreAtendimentosMinutos ?? 0);
    this.uf.set(dto.uf ?? '');
    const uniforme = lista.every(
      (dia) => dia.aberto && dia.abre === this.abre() && dia.fecha === this.fecha() && dia.duracao === null
    );
    this.personalizarAberto.set(recebidos.length > 0 && !uniforme);
  }

  private paraDiaEspecial(dto: DataEspecialDTO): DiaEspecial {
    return {
      id: dto.id,
      data: dto.data,
      fechado: dto.fechado,
      abre: dto.fechado ? '' : hora(dto.abre),
      fecha: dto.fechado ? '' : hora(dto.fecha),
      motivo: dto.motivo ?? ''
    };
  }

  private falhaDeAcao(titulo: string, erro: unknown): void {
    this.salvando.set(false);
    this.erroAcao.set({ titulo, texto: mensagemDoServidor(erro) ?? this.conteudo.erro.texto });
  }

  private limparNovo(): void {
    this.novaData.set('');
    this.novoMotivo.set('');
    this.novoFechado.set(true);
    this.novoAbre.set('');
    this.novoFecha.set('');
  }

  private fechaDepois(abre: string, fecha: string): boolean {
    return abre !== '' && fecha !== '' && fecha > abre;
  }
}
