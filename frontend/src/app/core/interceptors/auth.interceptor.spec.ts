import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor, mensagemHttp } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor e mensagemHttp', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['token']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('anexa cabecalho Authorization Bearer quando token existe', () => {
    authServiceSpy.token.and.returnValue('meu-jwt-token');

    http.get('/api/teste').subscribe();

    const req = httpMock.expectOne('/api/teste');
    expect(req.request.headers.get('Authorization')).toBe('Bearer meu-jwt-token');
    req.flush({});
  });

  it('nao anexa cabecalho Authorization quando token e nulo', () => {
    authServiceSpy.token.and.returnValue(null);

    http.get('/api/teste').subscribe();

    const req = httpMock.expectOne('/api/teste');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('mensagemHttp traduz corretamente status de erro e payloads', () => {
    expect(mensagemHttp('erro generico')).toBe('Não foi possível concluir a operação.');

    const erro0 = new HttpErrorResponse({ status: 0 });
    expect(mensagemHttp(erro0)).toBe('API indisponível. Confira se o Spring Boot está em http://localhost:8080.');

    const erro401Padrao = new HttpErrorResponse({ status: 401 });
    expect(mensagemHttp(erro401Padrao)).toBe('Não autorizado. Faça login novamente.');

    const erro401Custom = new HttpErrorResponse({ status: 401, error: { message: 'Token expirado' } });
    expect(mensagemHttp(erro401Custom)).toBe('Token expirado');

    const erro404Padrao = new HttpErrorResponse({ status: 404 });
    expect(mensagemHttp(erro404Padrao)).toBe('Recurso não encontrado.');

    const erro405 = new HttpErrorResponse({ status: 405 });
    expect(mensagemHttp(erro405)).toBe('A API rejeitou o método HTTP. Conectar WhatsApp usa POST em /whatsapp/{sessao}/iniciar.');

    const erro400Padrao = new HttpErrorResponse({ status: 400 });
    expect(mensagemHttp(erro400Padrao)).toBe('Sessão WhatsApp já ocupada ou dados inválidos.');

    const erro500Padrao = new HttpErrorResponse({ status: 500 });
    expect(mensagemHttp(erro500Padrao)).toBe('Erro 500 ao falar com a API.');
  });
});
