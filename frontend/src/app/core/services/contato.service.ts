import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ContatoResumoDTO, NumeroContatoDTO } from '../../shared/models/contato.models';

@Injectable({ providedIn: 'root' })
export class ContatoService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly base = `${environment.apiUrl}/empresas`;

  private obterSessao(sessao?: string): string {
    return sessao || this.authService.sessao()?.sessaoWhatsapp || '';
  }

  listar(pagina: number = 0, tamanho: number = 10, sessao?: string): Observable<ContatoResumoDTO[]> {
    const s = this.obterSessao(sessao);
    const params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanho', tamanho.toString());
    return this.http.get<ContatoResumoDTO[]>(`${this.base}/${s}/contatos`, { params });
  }

  revelarNumero(contatoId: number, sessao?: string): Observable<NumeroContatoDTO> {
    const s = this.obterSessao(sessao);
    return this.http.post<NumeroContatoDTO>(`${this.base}/${s}/contatos/${contatoId}/numero`, {});
  }

  excluir(contatoId: number, sessao?: string): Observable<void> {
    const s = this.obterSessao(sessao);
    return this.http.delete<void>(`${this.base}/${s}/contatos/${contatoId}`);
  }
}
