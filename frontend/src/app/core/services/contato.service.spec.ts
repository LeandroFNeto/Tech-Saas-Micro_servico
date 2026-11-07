import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ContatoService } from './contato.service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { ContatoResumoDTO, NumeroContatoDTO } from '../../shared/models/contato.models';

describe('ContatoService', () => {
  let service: ContatoService;
  let httpMock: HttpTestingController;

  const authServiceMock = {
    sessao: signal({ sessaoWhatsapp: 'sessao_teste_01' })
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock }
      ]
    });

    service = TestBed.inject(ContatoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listar faz GET com parametros de pagina e tamanho usando sessao', () => {
    const listaMock: ContatoResumoDTO[] = [
      { id: 1, nome: 'Ana', telefoneMascarado: '(11) *****-1234', naoContatar: false, origem: 'CONVERSA' }
    ];

    service.listar(1, 20).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].nome).toBe('Ana');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_teste_01/contatos?pagina=1&tamanho=20`);
    expect(req.request.method).toBe('GET');
    req.flush(listaMock);
  });

  it('revelarNumero faz POST para endpoint de numero', () => {
    const mockResposta: NumeroContatoDTO = { numero: '5511999998888' };

    service.revelarNumero(10).subscribe((res) => {
      expect(res.numero).toBe('5511999998888');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_teste_01/contatos/10/numero`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResposta);
  });

  it('excluir faz DELETE para remover contato', () => {
    service.excluir(15).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_teste_01/contatos/15`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
