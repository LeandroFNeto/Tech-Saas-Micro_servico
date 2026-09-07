import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessaoStatusResponseDTO } from '../../shared/models/whatsapp.models';

@Injectable({ providedIn: 'root' })
export class WhatsappConnectionService {
  private readonly base = `${environment.apiUrl}/whatsapp`;

  constructor(private readonly http: HttpClient) {}

  consultarStatus(sessao: string): Observable<SessaoStatusResponseDTO> {
    return this.http.get<SessaoStatusResponseDTO>(`${this.base}/status/${encodeURIComponent(sessao)}`);
  }

  iniciar(sessao: string): Observable<SessaoStatusResponseDTO> {
    return this.http.post<SessaoStatusResponseDTO>(`${this.base}/iniciar/${encodeURIComponent(sessao)}`, {});
  }
}
