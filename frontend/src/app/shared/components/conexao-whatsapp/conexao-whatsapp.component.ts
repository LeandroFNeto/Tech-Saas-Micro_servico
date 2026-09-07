import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, ReplaySubject, catchError, filter, of, switchMap, takeWhile, tap, timer } from 'rxjs';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { SessaoStatusResponseDTO, StatusSessaoWhatsapp } from '../../models/whatsapp.models';

@Component({
  selector: 'app-conexao-whatsapp',
  templateUrl: './conexao-whatsapp.component.html'
})
export class ConexaoWhatsappComponent implements OnChanges {
  @Input({ required: true }) sessao = '';

  private readonly api = inject(WhatsappConnectionService);
  private readonly sessao$ = new ReplaySubject<string>(1);

  readonly status = signal<StatusSessaoWhatsapp>('DISCONNECTED');
  readonly qrcode = signal<string | null>(null);
  readonly carregando = signal(false);
  readonly iniciando = signal(false);
  readonly erro = signal('');

  constructor() {
    this.sessao$
      .pipe(
        switchMap((sessao) => this.observarStatus(sessao)),
        takeUntilDestroyed()
      )
      .subscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessao']) {
      this.sessao$.next(this.sessao);
    }
  }

  iniciarConexao(): void {
    if (!this.sessao) {
      return;
    }

    this.iniciando.set(true);
    this.erro.set('');
    this.api.iniciar(this.sessao).subscribe({
      next: (dto) => {
        this.aplicar(dto);
        this.iniciando.set(false);
        this.sessao$.next(this.sessao);
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.iniciando.set(false);
      }
    });
  }

  srcQrcode(): string | null {
    const bruto = this.qrcode();
    if (!bruto) {
      return null;
    }
    return bruto.startsWith('data:') ? bruto : `data:image/png;base64,${bruto}`;
  }

  private observarStatus(sessao: string) {
    if (!sessao) {
      return EMPTY;
    }

    if (this.status() !== 'CONNECTED' && this.status() !== 'QRCODE') {
      this.carregando.set(true);
    }
    return timer(0, 3000).pipe(
      switchMap(() =>
        this.api.consultarStatus(sessao).pipe(
          catchError((err) => {
            this.erro.set(mensagemHttp(err));
            this.carregando.set(false);
            return of(null);
          })
        )
      ),
      filter((dto): dto is SessaoStatusResponseDTO => dto != null),
      tap((dto) => {
        this.aplicar(dto);
        this.carregando.set(false);
      }),
      takeWhile((dto) => dto.status !== 'CONNECTED', true)
    );
  }

  private aplicar(dto: SessaoStatusResponseDTO): void {
    this.status.set(dto.status);
    this.qrcode.set(dto.qrcodeBase64);
    this.erro.set('');
  }
}
