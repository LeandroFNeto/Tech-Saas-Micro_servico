import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AvisoService } from './aviso.service';
import { environment } from '../../../environments/environment';
import { AvisoDTO } from '../../shared/models/aviso.models';

describe('AvisoService', () => {
  let service: AvisoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AvisoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listarAtivos faz requisicao GET para endpoint publico', () => {
    const listaMock: AvisoDTO[] = [
      { id: 1, tipo: 'MANUTENCAO', titulo: 'Manutenção', conteudo: 'Instabilidade', ativo: true }
    ];

    service.listarAtivos().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].titulo).toBe('Manutenção');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/avisos`);
    expect(req.request.method).toBe('GET');
    req.flush(listaMock);
  });

  it('listarAdmin faz requisicao GET para endpoint admin', () => {
    service.listarAdmin().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/admin/avisos`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('criar faz POST com novo aviso', () => {
    const novoAviso: AvisoDTO = { tipo: 'COMUNICADO', titulo: 'Novo recurso', conteudo: 'Teste', ativo: true };

    service.criar(novoAviso).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/avisos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(novoAviso);
    req.flush({ ...novoAviso, id: 10 });
  });

  it('atualizar faz PUT com id no caminho', () => {
    const aviso: AvisoDTO = { id: 5, tipo: 'TUTORIAL', titulo: 'Tutorial atualizado', conteudo: 'Guia', ativo: true };

    service.atualizar(5, aviso).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/avisos/5`);
    expect(req.request.method).toBe('PUT');
    req.flush(aviso);
  });

  it('excluir faz DELETE com id no caminho', () => {
    service.excluir(5).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/avisos/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
