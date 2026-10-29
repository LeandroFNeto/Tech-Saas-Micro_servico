import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DataEspecialCreateDTO,
  DataEspecialDTO,
  FuncionamentoDTO,
  FuncionamentoUpdateDTO
} from '../../shared/models/funcionamento.models';

@Injectable({ providedIn: 'root' })
export class FuncionamentoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/empresas`;

  ler(sessao: string): Observable<FuncionamentoDTO> {
    return this.http.get<FuncionamentoDTO>(this.url(sessao));
  }

  salvar(sessao: string, dto: FuncionamentoUpdateDTO): Observable<FuncionamentoDTO> {
    return this.http.put<FuncionamentoDTO>(this.url(sessao), dto);
  }

  cadastrarDiaEspecial(sessao: string, dto: DataEspecialCreateDTO): Observable<DataEspecialDTO> {
    return this.http.post<DataEspecialDTO>(this.diasEspeciais(sessao), dto);
  }

  removerDiaEspecial(sessao: string, id: number): Observable<void> {
    return this.http.delete<void>(`${this.diasEspeciais(sessao)}/${id}`);
  }

  private url(sessao: string): string {
    return `${this.base}/${encodeURIComponent(sessao)}/funcionamento`;
  }

  private diasEspeciais(sessao: string): string {
    return `${this.base}/${encodeURIComponent(sessao)}/dias-especiais`;
  }
}
