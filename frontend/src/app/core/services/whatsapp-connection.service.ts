import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CodigoPareamentoRequestDTO,
  CodigoPareamentoResponseDTO,
  SessaoStatusResponseDTO
} from '../../shared/models/whatsapp.models';

@Injectable({ providedIn: 'root' })
export class WhatsappConnectionService {
  private readonly base = `${environment.apiUrl}/whatsapp`;

  constructor(private readonly http: HttpClient) {}

  consultarStatus(sessao: string): Observable<SessaoStatusResponseDTO> {
    return this.http.get<SessaoStatusResponseDTO>(
      `${this.base}/${encodeURIComponent(sessao)}/status`
    );
  }

  iniciar(sessao: string): Observable<SessaoStatusResponseDTO> {
    return this.http.post<SessaoStatusResponseDTO>(
      `${this.base}/${encodeURIComponent(sessao)}/iniciar`,
      {}
    );
  }

  solicitarCodigoPareamento(sessao: string, telefone: string): Observable<CodigoPareamentoResponseDTO> {
    const corpo: CodigoPareamentoRequestDTO = { telefone };
    return this.http.post<CodigoPareamentoResponseDTO>(
      `${this.base}/${encodeURIComponent(sessao)}/codigo-pareamento`,
      corpo
    );
  }
}
