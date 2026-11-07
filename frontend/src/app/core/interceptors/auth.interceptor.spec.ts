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

  it('anexa cabecalho Authorization Bearer em rotas relativas da API quando token valido existe', () => {
    authServiceSpy.token.and.returnValue('meu-jwt-token');

    http.get('/api/teste').subscribe();

    const req = httpMock.expectOne('/api/teste');
    expect(req.request.headers.get('Authorization')).toBe('Bearer meu-jwt-token');
    req.flush({});
  });

  it('nao anexa cabecalho Authorization quando token e nulo ou expirado', () => {
    authServiceSpy.token.and.returnValue(null);

    http.get('/api/teste').subscribe();

    const req = httpMock.expectOne('/api/teste');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('nao vaza cabecalho Authorization para dominios externos como Cloudinary', () => {
    authServiceSpy.token.and.returnValue('jwt-secreto');

    const urlCloudinary = 'https://api.cloudinary.com/v1_1/demo/image/upload';
    http.post(urlCloudinary, {}).subscribe();

    const req = httpMock.expectOne(urlCloudinary);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('mensagemHttp traduz corretamente status HTTP 0, 400, 401, 403, 404, 405, 422, 429 e 500 sem expor localhost', () => {
    expect(mensagemHttp('erro generico')).toBe('Não foi possível concluir a operação.');

    const erro0 = new HttpErrorResponse({ status: 0 });
    expect(mensagemHttp(erro0)).not.toContain('localhost:8080');
    expect(mensagemHttp(erro0)).toContain('indisponível');

    expect(mensagemHttp(new HttpErrorResponse({ status: 400 }))).toBe('Dados inválidos. Confira os campos informados.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 401 }))).toBe('Não autorizado. Faça login novamente.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 401, error: { message: 'Token expirado' } }))).toBe('Token expirado');
    expect(mensagemHttp(new HttpErrorResponse({ status: 403 }))).toBe('Acesso negado para esta operação.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 404 }))).toBe('Recurso não encontrado.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 405 }))).toBe('Método HTTP não permitido para este recurso.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 422 }))).toBe('Dados inválidos para processamento.');
    expect(mensagemHttp(new HttpErrorResponse({ status: 429 }))).toContain('Muitas requisições');
    expect(mensagemHttp(new HttpErrorResponse({ status: 500 }))).toBe('Erro 500 ao falar com a API.');
  });
});
