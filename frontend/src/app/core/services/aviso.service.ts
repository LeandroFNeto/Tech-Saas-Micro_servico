import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvisoDTO } from '../../shared/models/aviso.models';

@Injectable({ providedIn: 'root' })
export class AvisoService {
  private readonly publico = `${environment.apiUrl}/avisos`;
  private readonly admin = `${environment.apiUrl}/admin/avisos`;

  constructor(private readonly http: HttpClient) {}

  listarAtivos(): Observable<AvisoDTO[]> {
    return this.http.get<AvisoDTO[]>(this.publico);
  }

  listarAdmin(): Observable<AvisoDTO[]> {
    return this.http.get<AvisoDTO[]>(this.admin);
  }

  criar(dto: AvisoDTO): Observable<AvisoDTO> {
    return this.http.post<AvisoDTO>(this.admin, dto);
  }

  atualizar(id: number, dto: AvisoDTO): Observable<AvisoDTO> {
    return this.http.put<AvisoDTO>(`${this.admin}/${id}`, dto);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/${id}`);
  }
}
