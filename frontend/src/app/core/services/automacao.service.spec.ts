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

  it('deve listar reservas da sessao com telefoneMascarado', () => {
    service.listarReservas('teste-sessao').subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].nomeCliente).toBe('Carlos');
      expect(res[0].telefoneMascarado).toBe('(11) *****-1234');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/reservas`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 1,
        data: '2026-10-30',
        horario: '14:00',
        nomeCliente: 'Carlos',
        telefoneMascarado: '(11) *****-1234',
        situacao: 'PRONTA_PARA_AVISAR',
        podeAvisar: true
      }
    ]);
  });

  it('deve enviar lembrete manual', () => {
    service.enviarLembrete('teste-sessao', 10).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/reservas/10/lembrete`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('deve vincular contato enviando campo telefone ou contatoId conforme OpenAPI', () => {
    service.vincularContato('teste-sessao', 7, { telefone: '11987654321' }).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/reservas/7/contato`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ telefone: '11987654321' });
    req.flush(null);
  });

  it('deve listar contatos da sessao com telefoneMascarado', () => {
    service.listarContatos('teste-sessao').subscribe((contatos) => {
      expect(contatos.length).toBe(1);
      expect(contatos[0].nome).toBe('Maria');
      expect(contatos[0].telefoneMascarado).toBe('(11) *****-9999');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/contatos`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 2,
        nome: 'Maria',
        telefoneMascarado: '(11) *****-9999',
        primeiraConversaEm: '2026-10-01T10:00:00',
        ultimaConversaEm: '2026-10-20T15:00:00',
        origem: 'CONVERSA'
      }
    ]);
  });

  it('deve obter numero completo descriptografado do contato', () => {
    service.obterNumeroContato('teste-sessao', 2).subscribe((dto) => {
      expect(dto.numero).toBe('5511987659999');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/teste-sessao/contatos/2/numero`);
    expect(req.request.method).toBe('GET');
    req.flush({ numero: '5511987659999' });
  });
});
