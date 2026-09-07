import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { EmpresaService } from '../../../core/services/empresa.service';
import { EmpresaView } from '../../../shared/models/empresa.models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly empresasApi = inject(EmpresaService);

  readonly empresas = signal<EmpresaView[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal('');

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.empresasApi.listar().subscribe({
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
}
