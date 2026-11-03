import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssistenteChatRequestDTO,
  AssistenteChatResponseDTO,
  DiagnosticoResponseDTO
} from '../../shared/models/assistente.models';

@Injectable({ providedIn: 'root' })
export class AssistenteTutoriaService {
  private readonly baseUrl = `${environment.apiUrl}/admin/assistente-tutoria`;
  private readonly clienteUrl = `${environment.apiUrl}/api/cliente/assistente`;

  constructor(private readonly http: HttpClient) {}

  executarDiagnostico(): Observable<DiagnosticoResponseDTO> {
    return this.http.get<DiagnosticoResponseDTO>(`${this.baseUrl}/diagnostico`);
  }

  conversar(requisicao: AssistenteChatRequestDTO): Observable<AssistenteChatResponseDTO> {
    return this.http.post<AssistenteChatResponseDTO>(`${this.baseUrl}/chat`, requisicao);
  }

  conversarCliente(requisicao: AssistenteChatRequestDTO): Observable<AssistenteChatResponseDTO> {
    return this.http.post<AssistenteChatResponseDTO>(`${this.clienteUrl}/chat`, requisicao);
  }
}
