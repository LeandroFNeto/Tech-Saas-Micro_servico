import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, of, switchMap, takeUntil, timer } from 'rxjs';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
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
  private readonly empresasApi = inject(EmpresaService);
  private readonly whatsapp = inject(WhatsappConnectionService);
  private readonly destroy$ = new Subject<void>();

  readonly empresas = signal<EmpresaView[]>([]);
  readonly statusPorSessao = signal<Record<string, StatusSessaoWhatsapp>>({});
  readonly carregando = signal(true);
  readonly erro = signal('');

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
