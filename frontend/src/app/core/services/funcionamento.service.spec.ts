import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { DataEspecialDTO, FuncionamentoDTO } from '../../shared/models/funcionamento.models';
import { FuncionamentoService } from './funcionamento.service';

describe('FuncionamentoService', () => {
  const base = `${environment.apiUrl}/empresas/quadra`;
  let servico: FuncionamentoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servico = TestBed.inject(FuncionamentoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('le o funcionamento em GET /empresas/{sessao}/funcionamento', () => {
    const resposta: FuncionamentoDTO = {
      duracaoAtendimentoMinutos: 60,
      intervaloEntreAtendimentosMinutos: 0,
      uf: null,
      horarios: [],
      diasEspeciais: []
    };
    let recebida: FuncionamentoDTO | undefined;
    servico.ler('quadra').subscribe((valor) => (recebida = valor));
    const req = http.expectOne(`${base}/funcionamento`);
    expect(req.request.method).toBe('GET');
    req.flush(resposta);
    expect(recebida).toEqual(resposta);
  });

  it('salva em PUT /empresas/{sessao}/funcionamento com o corpo recebido', () => {
    const dto = {
      duracaoAtendimentoMinutos: 90,
      intervaloEntreAtendimentosMinutos: 15,
      uf: 'SP',
      horarios: [{ perfil: 'SEGUNDA' as const, fechado: false, abre: '08:00', fecha: '22:00', duracaoMinutos: null }]
    };
    servico.salvar('quadra', dto).subscribe();
    const req = http.expectOne(`${base}/funcionamento`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dto);
    req.flush({ ...dto, diasEspeciais: [] });
  });

  it('cadastra o dia especial em POST /empresas/{sessao}/dias-especiais', () => {
    const resposta: DataEspecialDTO = { id: 5, data: '2026-12-08', fechado: true, reservasNaData: ['x'] };
    let recebida: DataEspecialDTO | undefined;
    servico.cadastrarDiaEspecial('quadra', { data: '2026-12-08', fechado: true }).subscribe((v) => (recebida = v));
    const req = http.expectOne(`${base}/dias-especiais`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ data: '2026-12-08', fechado: true });
    req.flush(resposta, { status: 201, statusText: 'Created' });
    expect(recebida).toEqual(resposta);
  });

  it('remove o dia especial em DELETE /empresas/{sessao}/dias-especiais/{id}', () => {
    let concluiu = false;
    servico.removerDiaEspecial('quadra', 5).subscribe(() => (concluiu = true));
    const req = http.expectOne(`${base}/dias-especiais/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(concluiu).toBeTrue();
  });

  it('propaga o erro HTTP', () => {
    let status = 0;
    servico.ler('quadra').subscribe({ error: (erro) => (status = erro.status) });
    http.expectOne(`${base}/funcionamento`).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(status).toBe(403);
  });
});
