import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LembreteConfigDTO {
  modo: 'DESLIGADO' | 'SUGERIDO' | 'AUTOMATICO';
  diasAntes: number;
  horarioEnvio: string;
  texto: string;
}

export interface ReservaResumoDTO {
  id: number;
  data: string;
  horario: string;
  nomeCliente: string;
  telefoneMascara: string;
  situacao: string;
  podeAvisar: boolean;
  contatoId?: number;
}

export interface ContatoResumoDTO {
  id: number;
  nome: string;
  telefoneMascara: string;
  primeiraConversaEm: string;
  ultimaConversaEm: string;
  origem: string;
}

export interface VincularContatoDTO {
  contatoId?: number;
  numeroTelefone?: string;
}

export interface NumeroContatoDTO {
  numero: string;
}

@Injectable({ providedIn: 'root' })
export class AutomacaoService {
  private readonly base = `${environment.apiUrl}/empresas`;

  constructor(private readonly http: HttpClient) {}

  listarReservas(sessao: string): Observable<ReservaResumoDTO[]> {
    return this.http.get<ReservaResumoDTO[]>(`${this.base}/${sessao}/reservas`);
  }

  enviarLembrete(sessao: string, reservaId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/${sessao}/reservas/${reservaId}/lembrete`, {});
  }

  vincularContato(sessao: string, reservaId: number, dto: VincularContatoDTO): Observable<void> {
    return this.http.post<void>(`${this.base}/${sessao}/reservas/${reservaId}/contato`, dto);
  }

  listarContatos(sessao: string): Observable<ContatoResumoDTO[]> {
    return this.http.get<ContatoResumoDTO[]>(`${this.base}/${sessao}/contatos`);
  }

  obterNumeroContato(sessao: string, contatoId: number): Observable<NumeroContatoDTO> {
    return this.http.get<NumeroContatoDTO>(`${this.base}/${sessao}/contatos/${contatoId}/numero`);
  }
}
