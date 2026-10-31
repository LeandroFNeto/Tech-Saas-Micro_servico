import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, of, switchMap, takeUntil, timer } from 'rxjs';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { EmpresaView } from '../../../shared/models/empresa.models';
import { StatusSessaoWhatsapp } from '../../../shared/models/whatsapp.models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly empresasApi = inject(EmpresaService);
  private readonly whatsapp = inject(WhatsappConnectionService);
  private readonly destroy$ = new Subject<void>();

  readonly empresas = signal<EmpresaView[]>([]);
  readonly statusPorSessao = signal<Record<string, StatusSessaoWhatsapp>>({});
  readonly carregando = signal(true);
  readonly erro = signal('');
  readonly empresaExcluindo = signal<EmpresaView | null>(null);
  readonly confirmacaoAberta = signal(false);
  readonly excluindo = signal(false);

  ngOnInit(): void {
    this.carregar();
    timer(0, 5000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.consultarSessoes())
      )
      .subscribe((lista) => {
        const mapa: Record<string, StatusSessaoWhatsapp> = {};
        for (const dto of lista) {
          mapa[dto.sessao] = dto.status;
        }
        this.statusPorSessao.set(mapa);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.empresasApi
      .listar()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (lista) => {
          this.empresas.set(lista);
          this.carregando.set(false);
        },
        error: (err) => {
          this.erro.set(mensagemHttp(err));
          this.carregando.set(false);
        }
      });
  }

  abrirConfirmacaoExclusao(empresa: EmpresaView): void {
    this.empresaExcluindo.set(empresa);
    this.confirmacaoAberta.set(true);
  }

  cancelarExclusao(): void {
    this.confirmacaoAberta.set(false);
    this.empresaExcluindo.set(null);
  }

  confirmarExclusao(): void {
    const empresa = this.empresaExcluindo();
    if (!empresa) return;

    this.excluindo.set(true);
    this.empresasApi.excluir(empresa.sessaoWhatsapp).subscribe({
      next: () => {
        this.excluindo.set(false);
        this.confirmacaoAberta.set(false);
        this.empresaExcluindo.set(null);
        this.carregar();
      },
      error: (err) => {
        this.excluindo.set(false);
        this.erro.set(mensagemHttp(err));
      }
    });
  }

  statusDe(sessao: string): StatusSessaoWhatsapp | null {
    return this.statusPorSessao()[sessao] ?? null;
  }

  private consultarSessoes() {
    const sessoes = this.empresas()
      .map((empresa) => empresa.sessaoWhatsapp)
      .filter((sessao) => !!sessao);
    if (sessoes.length === 0) {
      return of([]);
    }
    return forkJoin(
      sessoes.map((sessao) =>
        this.whatsapp.consultarStatus(sessao).pipe(
          catchError(() => of({ sessao, status: 'DISCONNECTED' as const, qrcodeBase64: null }))
        )
      )
    );
  }
}
