import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AutomacaoService } from './automacao.service';
import { environment } from '../../../environments/environment';

describe('AutomacaoService', () => {
  let service: AutomacaoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AutomacaoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve listar reservas da sessao', () => {
    service.listarReservas('teste-sessao').subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].nomeCliente).toBe('Carlos');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/reservas`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, data: '2026-10-30', horario: '14:00', nomeCliente: 'Carlos', telefoneMascara: '(11) 9****-1234', situacao: 'PRONTA_PARA_AVISAR', podeAvisar: true }]);
  });

  it('deve enviar lembrete manual', () => {
    service.enviarLembrete('teste-sessao', 10).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/reservas/10/lembrete`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('deve listar contatos da sessao', () => {
    service.listarContatos('teste-sessao').subscribe((contatos) => {
      expect(contatos.length).toBe(1);
      expect(contatos[0].nome).toBe('Maria');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/contatos`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 2, nome: 'Maria', telefoneMascara: '(11) 9****-9999', primeiraConversaEm: '2026-10-01', ultimaConversaEm: '2026-10-20', origem: 'BOT' }]);
  });
});
