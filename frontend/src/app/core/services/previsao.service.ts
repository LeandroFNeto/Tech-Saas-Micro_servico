import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PrevisaoTempoResponse } from '../../shared/models/previsao.models';

@Injectable({ providedIn: 'root' })
export class PrevisaoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/empresas`;

  buscar(sessao: string): Observable<PrevisaoTempoResponse> {
    return this.http.get<PrevisaoTempoResponse>(`${this.base}/${sessao}/previsao`);
  }
}
