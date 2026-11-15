import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  effect,
  inject,
  signal,
  untracked,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  EMPTY,
  ReplaySubject,
  Subject,
  Subscription,
  catchError,
  distinctUntilChanged,
  filter,
  interval,
  of,
  switchMap,
  takeUntil,
  takeWhile,
  tap,
  timer
} from 'rxjs';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { SessaoStatusResponseDTO, StatusSessaoWhatsapp } from '../../models/whatsapp.models';
import contentData from '../../../../../../especificacao/front/conexao-whatsapp.content.json';
import designData from '../../../../../../especificacao/front/telas.design.json';
import { formatarTelefone, formatarTempo, somenteDigitos, telefoneValido } from './conexao-whatsapp.telefone';

export const VALIDADE_CODIGO_SEGUNDOS = 120;
export const INTERVALO_STATUS_MS = 1500;
export const INTERVALO_STATUS_LENTO_MS = 5000;
export const GUARDA_DESCONECTADO_MS = 45_000;
export const ESPERA_REPETICAO_409_MS = 2000;
export const REPETICOES_409 = 3;
export const AVISO_CODIGO_ULTIMOS_SEGUNDOS = 30;
export const DURACAO_COPIADO_MS = 2000;

export type PassoConexao =
  | 'inicio'
  | 'telefone'
  | 'codigo'
  | 'codigo-expirado'
  | 'qrcode'
  | 'conectando'
  | 'reconectando'
  | 'conectado'
  | 'repareamento';

export type ErroConexao =
  | '400'
  | '403'
  | '404'
  | '409Preparando'
  | '409Iniciando'
  | '429'
  | '503'
  | 'generico';

const PASSOS_COM_PAREAMENTO: PassoConexao[] = ['codigo', 'codigo-expirado', 'qrcode', 'conectando'];
const PASSOS_COM_TELEFONE: PassoConexao[] = ['telefone', 'codigo', 'codigo-expirado'];
const PASSOS_COM_ETAPAS: PassoConexao[] = ['telefone', 'codigo', 'codigo-expirado', 'conectando'];

function codigoHttp(erro: unknown): number {
  if (erro instanceof HttpErrorResponse) {
    return erro.status;
  }
  return (erro as { status?: number } | null)?.status ?? 0;
}

@Component({
  selector: 'app-conexao-whatsapp',
  templateUrl: './conexao-whatsapp.component.html'
})
export class ConexaoWhatsappComponent implements OnChanges {
  @Input({ required: true }) sessao = '';
  @Input() compacto = false;

  readonly conteudo = contentData.conexaoWhatsapp;
  readonly design = designData.telas.conexaoWhatsapp;
  readonly etapaPorPasso: Partial<Record<PassoConexao, number>> = {
    telefone: 1,
    codigo: 2,
    'codigo-expirado': 2,
    conectando: 3
  };

  private readonly api = inject(WhatsappConnectionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sessao$ = new ReplaySubject<string>(1);
  private readonly ritmo$ = new BehaviorSubject<number | null>(INTERVALO_STATUS_MS);
  private readonly cancelar$ = new Subject<void>();
  private readonly codigoRef = viewChild<ElementRef<HTMLElement>>('codigoValor');

  readonly passo = signal<PassoConexao>('inicio');
  readonly status = signal<StatusSessaoWhatsapp>('DISCONNECTED');
  readonly qrcode = signal<string | null>(null);
  readonly carregando = signal(false);
  readonly iniciando = signal(false);
  readonly gerando = signal(false);
  readonly erro = signal<ErroConexao | null>(null);
  readonly semInternet = signal(false);
  readonly telefone = signal('');
  readonly codigo = signal('');
  readonly segundosRestantes = signal(0);
  readonly copiado = signal(false);
  readonly anuncio = signal('');
  readonly codigoIndisponivel = signal(false);
  readonly pedidoFoco = signal(0);

  private observacaoParada = false;
  private telefoneDigitos = '';
  private guardaAte = 0;
  private fimDoCodigo = 0;
  private aba = true;
  private contagem: Subscription | null = null;
  private repeticao: Subscription | null = null;
  private fimCopiado: Subscription | null = null;

  constructor() {
    this.sessao$
      .pipe(
        switchMap((sessao) => this.observarStatus(sessao)),
        takeUntilDestroyed()
      )
      .subscribe();

    effect(() => {
      this.pedidoFoco();
      const alvo = this.codigoRef();
      if (alvo) {
        untracked(() => queueMicrotask(() => alvo.nativeElement.focus()));
      }
    });

    this.semInternet.set(typeof navigator !== 'undefined' && navigator.onLine === false);
    this.aba = typeof document === 'undefined' || !document.hidden;
    this.ouvirAmbiente();
    this.atualizarRitmo();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessao']) {
      this.reiniciarTela();
      this.sessao$.next(this.sessao);
    }
  }

  escolherCodigo(): void {
    this.telefone.set('');
    this.telefoneDigitos = '';
    this.ir('telefone');
  }

  voltarInicio(): void {
    this.ir('inicio');
  }

  voltarParaCodigo(): void {
    this.codigoIndisponivel.set(false);
    this.escolherCodigo();
  }

  aoDigitarTelefone(valor: string): string {
    const digitos = somenteDigitos(valor);
    const formatado = formatarTelefone(digitos);
    this.telefone.set(formatado);
    if (this.erro() === '400') {
      this.erro.set(null);
    }
    return formatado;
  }

  gerarCodigo(): void {
    if (this.gerando() || !this.sessao) {
      return;
    }
    const digitos = somenteDigitos(this.telefone());
    if (!telefoneValido(digitos)) {
      this.erro.set('400');
      return;
    }
    this.telefoneDigitos = digitos;
    this.erro.set(null);
    this.gerando.set(true);
    this.retomarObservacao();
    this.guardaAte = Date.now() + GUARDA_DESCONECTADO_MS;
    this.api
      .iniciar(this.sessao)
      .pipe(takeUntil(this.cancelar$), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dto) => {
          if (dto.status === 'CONNECTED') {
            this.gerando.set(false);
            this.aplicar(dto);
            return;
          }
          this.pedirCodigo(0);
        },
        error: (err) => this.aoErroDeAcao(err)
      });
  }

  gerarOutroCodigo(): void {
    if (this.gerando() || !this.telefoneDigitos) {
      return;
    }
    this.erro.set(null);
    this.gerando.set(true);
    this.guardaAte = Date.now() + GUARDA_DESCONECTADO_MS;
    this.pedirCodigo(0);
  }

  tentarDeNovo(): void {
    this.gerarOutroCodigo();
  }

  usarQrcode(): void {
    if (this.iniciando() || !this.sessao) {
      return;
    }
    this.cancelar$.next();
    this.pararRepeticao();
    this.gerando.set(false);
    this.retomarObservacao();
    this.iniciando.set(true);
    this.guardaAte = Date.now() + GUARDA_DESCONECTADO_MS;
    this.ir('qrcode');
    this.api
      .iniciar(this.sessao)
      .pipe(takeUntil(this.cancelar$), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dto) => {
          this.iniciando.set(false);
          this.aplicar(dto);
        },
        error: (err) => {
          this.iniciando.set(false);
          this.aoErroDeAcao(err);
        }
      });
  }

  async copiar(): Promise<void> {
    const codigo = this.codigo();
    if (!codigo) {
      return;
    }
    try {
      await navigator.clipboard.writeText(codigo);
    } catch {
      return;
    }
    this.copiado.set(true);
    this.fimCopiado?.unsubscribe();
    this.fimCopiado = timer(DURACAO_COPIADO_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.copiado.set(false));
  }

  srcQrcode(): string | null {
    const bruto = this.qrcode();
    if (!bruto) {
      return null;
    }
    return bruto.startsWith('data:') ? bruto : `data:image/png;base64,${bruto}`;
  }

  tempoRestante(): string {
    return formatarTempo(this.segundosRestantes());
  }

  porcentagemRestante(): number {
    return Math.max(0, Math.min(100, (this.segundosRestantes() / VALIDADE_CODIGO_SEGUNDOS) * 100));
  }

  nosUltimosSegundos(): boolean {
    return this.segundosRestantes() <= AVISO_CODIGO_ULTIMOS_SEGUNDOS;
  }

  etapaAtual(): number {
    return this.etapaPorPasso[this.passo()] ?? 0;
  }

  mostraEtapas(): boolean {
    return PASSOS_COM_ETAPAS.includes(this.passo());
  }

  textoEtapaCompacta(): string {
    const nome = this.conteudo.etapas.nomes[this.etapaAtual() - 1] ?? '';
    return this.conteudo.etapas.compacto.replace('{atual}', String(this.etapaAtual())).replace('{nome}', nome);
  }

  private ir(novo: PassoConexao): void {
    const anterior = this.passo();
    if (anterior === novo) {
      return;
    }
    this.passo.set(novo);
    this.erro.set(null);
    if (!PASSOS_COM_TELEFONE.includes(novo)) {
      this.limparFluxo();
    } else if (novo !== 'telefone') {
      this.telefone.set('');
    }
    this.atualizarRitmo();
  }

  private limparFluxo(): void {
    this.telefone.set('');
    this.telefoneDigitos = '';
    this.codigo.set('');
    this.copiado.set(false);
    this.anuncio.set('');
    this.gerando.set(false);
    this.pararContagem();
    this.pararRepeticao();
    this.codigoIndisponivel.set(false);
    if (this.passo() !== 'qrcode') {
      this.iniciando.set(false);
    }
  }

  private reiniciarTela(): void {
    this.cancelar$.next();
    this.guardaAte = 0;
    this.passo.set('inicio');
    this.status.set('DISCONNECTED');
    this.qrcode.set(null);
    this.erro.set(null);
    this.iniciando.set(false);
    this.limparFluxo();
    this.atualizarRitmo();
  }

  private pedirCodigo(repeticoes: number): void {
    this.api
      .solicitarCodigoPareamento(this.sessao, this.telefoneDigitos)
      .pipe(takeUntil(this.cancelar$), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resposta) => this.aoReceberCodigo(resposta.codigo),
        error: (err) => {
          if (codigoHttp(err) === 409) {
            this.aoConflito(repeticoes);
            return;
          }
          this.aoErroDeAcao(err);
        }
      });
  }

  private aoReceberCodigo(codigo: string): void {
    this.gerando.set(false);
    if (!PASSOS_COM_TELEFONE.includes(this.passo())) {
      return;
    }
    this.guardaAte = Date.now() + VALIDADE_CODIGO_SEGUNDOS * 1000 + 2 * INTERVALO_STATUS_MS;
    this.erro.set(null);
    this.copiado.set(false);
    this.codigo.set(codigo);
    this.telefone.set('');
    this.passo.set('codigo');
    this.iniciarContagem();
    this.pedidoFoco.update((n) => n + 1);
    this.atualizarRitmo();
  }

  private aoConflito(repeticoes: number): void {
    this.api
      .consultarStatus(this.sessao)
      .pipe(takeUntil(this.cancelar$), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dto) => {
          if (dto.status === 'CONNECTED') {
            this.aplicar(dto);
            return;
          }
          if (repeticoes >= REPETICOES_409) {
            this.gerando.set(false);
            this.erro.set('409Iniciando');
            return;
          }
          this.erro.set('409Preparando');
          this.pararRepeticao();
          this.repeticao = timer(ESPERA_REPETICAO_409_MS)
            .pipe(takeUntil(this.cancelar$), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.pedirCodigo(repeticoes + 1));
        },
        error: () => {
          this.gerando.set(false);
          this.erro.set('409Iniciando');
        }
      });
  }

  private aoErroDeAcao(erro: unknown): void {
    this.gerando.set(false);
    this.iniciando.set(false);
    this.pararRepeticao();
    switch (codigoHttp(erro)) {
      case 400:
        this.erro.set('400');
        break;
      case 401:
        this.limparTudo();
        break;
      case 403:
        this.ir('inicio');
        this.erro.set('403');
        break;
      case 404:
        this.ir('inicio');
        this.erro.set('404');
        break;
      case 429:
        this.erro.set('429');
        break;
      case 503:
        this.ir('qrcode');
        this.codigoIndisponivel.set(true);
        this.erro.set('503');
        break;
      default:
        this.erro.set('generico');
    }
  }

  private limparTudo(): void {
    this.cancelar$.next();
    this.guardaAte = 0;
    this.qrcode.set(null);
    this.passo.set('inicio');
    this.limparFluxo();
    this.erro.set(null);
    this.atualizarRitmo();
  }

  private iniciarContagem(): void {
    this.pararContagem();
    this.fimDoCodigo = Date.now() + VALIDADE_CODIGO_SEGUNDOS * 1000;
    this.segundosRestantes.set(VALIDADE_CODIGO_SEGUNDOS);
    this.contagem = interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.passarContagem());
  }

  private passarContagem(): void {
    const restante = Math.max(0, Math.ceil((this.fimDoCodigo - Date.now()) / 1000));
    this.segundosRestantes.set(restante);
    if (restante === 60) {
      this.anuncio.set(this.conteudo.codigo.anuncioMinuto);
    } else if (restante === AVISO_CODIGO_ULTIMOS_SEGUNDOS) {
      this.anuncio.set(this.conteudo.codigo.anuncioTrintaSegundos);
    }
    if (restante === 0) {
      this.pararContagem();
      if (this.passo() === 'codigo') {
        this.anuncio.set('');
        this.ir('codigo-expirado');
      }
    }
  }

  private pararContagem(): void {
    this.contagem?.unsubscribe();
    this.contagem = null;
    this.segundosRestantes.set(0);
  }

  private pararRepeticao(): void {
    this.repeticao?.unsubscribe();
    this.repeticao = null;
  }

  private ouvirAmbiente(): void {
    const aoMudarVisibilidade = (): void => {
      this.aba = !document.hidden;
      this.atualizarRitmo();
      if (this.aba && this.contagem) {
        this.passarContagem();
      }
    };
    const aoFicarOnline = (): void => {
      this.semInternet.set(false);
      this.atualizarRitmo();
    };
    const aoFicarOffline = (): void => {
      this.semInternet.set(true);
      this.atualizarRitmo();
    };
    document.addEventListener('visibilitychange', aoMudarVisibilidade);
    window.addEventListener('online', aoFicarOnline);
    window.addEventListener('offline', aoFicarOffline);
    this.destroyRef.onDestroy(() => {
      document.removeEventListener('visibilitychange', aoMudarVisibilidade);
      window.removeEventListener('online', aoFicarOnline);
      window.removeEventListener('offline', aoFicarOffline);
      this.cancelar$.next();
      this.pararContagem();
      this.pararRepeticao();
      this.fimCopiado?.unsubscribe();
    });
  }

  private atualizarRitmo(): void {
    if (this.semInternet()) {
      this.ritmo$.next(null);
      return;
    }
    const lento = !this.aba || this.passo() === 'conectado';
    this.ritmo$.next(lento ? INTERVALO_STATUS_LENTO_MS : INTERVALO_STATUS_MS);
  }

  private retomarObservacao(): void {
    if (this.observacaoParada && this.sessao) {
      this.observacaoParada = false;
      this.sessao$.next(this.sessao);
    }
  }

  private observarStatus(sessao: string) {
    if (!sessao) {
      return EMPTY;
    }

    this.carregando.set(true);
    let erroPermanente = false;
    this.observacaoParada = false;
    return this.ritmo$.pipe(
      distinctUntilChanged(),
      switchMap((ms) => (ms === null ? EMPTY : timer(0, ms))),
      switchMap(() =>
        this.api.consultarStatus(sessao).pipe(
          catchError((err: unknown) => {
            this.carregando.set(false);
            const codigo = codigoHttp(err);
            if (codigo === 401) {
              erroPermanente = true;
              this.observacaoParada = true;
              this.limparTudo();
            } else if (codigo === 403 || codigo === 404) {
              erroPermanente = true;
              this.observacaoParada = true;
              this.ir('inicio');
              this.erro.set(codigo === 403 ? '403' : '404');
            }
            return of(null);
          })
        )
      ),
      takeWhile(() => !erroPermanente, false),
      filter((dto): dto is SessaoStatusResponseDTO => dto != null),
      tap((dto) => {
        this.aplicar(dto);
        this.carregando.set(false);
      })
    );
  }

  private aplicar(dto: SessaoStatusResponseDTO): void {
    this.status.set(dto.status);
    if (dto.qrcodeBase64) {
      this.qrcode.set(dto.qrcodeBase64);
    } else if (dto.status !== 'QRCODE') {
      this.qrcode.set(null);
    }
    const proximo = this.passoPara(dto);
    if (proximo === 'conectado') {
      this.gerando.set(false);
      this.guardaAte = Date.now() + GUARDA_DESCONECTADO_MS;
    } else if (proximo === 'conectando' && dto.status === 'INITIALIZING') {
      this.guardaAte = Date.now() + GUARDA_DESCONECTADO_MS;
    }
    this.ir(proximo);
  }

  private passoPara(dto: SessaoStatusResponseDTO): PassoConexao {
    const atual = this.passo();
    const naGuarda = Date.now() < this.guardaAte;
    switch (dto.status) {
      case 'CONNECTED':
        return 'conectado';
      case 'INITIALIZING':
        if (atual === 'telefone') {
          return atual;
        }
        return PASSOS_COM_PAREAMENTO.includes(atual) ? 'conectando' : 'reconectando';
      case 'QRCODE':
        if (atual === 'telefone' || atual === 'codigo' || atual === 'codigo-expirado' || atual === 'qrcode') {
          return atual;
        }
        return atual === 'conectando' && naGuarda ? atual : 'inicio';
      default:
        if (atual === 'telefone') {
          return atual;
        }
        if (atual === 'codigo-expirado' && dto.sessaoEncerrada !== true) {
          return atual;
        }
        if (naGuarda && PASSOS_COM_PAREAMENTO.includes(atual)) {
          return atual;
        }
        if (dto.sessaoEncerrada === true) {
          return 'repareamento';
        }
        if (naGuarda && (atual === 'conectado' || atual === 'reconectando')) {
          return 'reconectando';
        }
        return 'inicio';
    }
  }
}
