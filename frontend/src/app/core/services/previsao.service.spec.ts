import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PrevisaoTempoResponse } from '../../shared/models/previsao.models';
import { PrevisaoService } from './previsao.service';

describe('PrevisaoService', () => {
  let servico: PrevisaoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servico = TestBed.inject(PrevisaoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('busca a previsao da sessao em GET /empresas/{sessao}/previsao', () => {
    const resposta: PrevisaoTempoResponse = {
      disponivel: true,
      reservasDisponiveis: true,
      atualizadoEm: '2026-10-27T14:30:00',
      cidade: 'Atibaia, São Paulo, BR',
      dias: []
    };
    let recebida: PrevisaoTempoResponse | undefined;
    servico.buscar('quadra').subscribe((valor) => (recebida = valor));

    const req = http.expectOne(`${environment.apiUrl}/empresas/quadra/previsao`);
    expect(req.request.method).toBe('GET');
    req.flush(resposta);
    expect(recebida).toEqual(resposta);
  });

  it('devolve disponivel=false sem erro quando o back responde indisponivel', () => {
    let recebida: PrevisaoTempoResponse | undefined;
    servico.buscar('quadra').subscribe((valor) => (recebida = valor));
    http
      .expectOne(`${environment.apiUrl}/empresas/quadra/previsao`)
      .flush({ disponivel: false, reservasDisponiveis: true, atualizadoEm: null, cidade: null, dias: [] });
    expect(recebida?.disponivel).toBe(false);
  });

  it('propaga o erro HTTP', () => {
    let status = 0;
    servico.buscar('quadra').subscribe({ error: (erro) => (status = erro.status) });
    http
      .expectOne(`${environment.apiUrl}/empresas/quadra/previsao`)
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    expect(status).toBe(503);
  });
});
