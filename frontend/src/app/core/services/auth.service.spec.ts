import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.models';

function paraBase64UrlUtf8(objeto: Record<string, unknown>): string {
  const bytes = new TextEncoder().encode(JSON.stringify(objeto));
  let binario = '';
  bytes.forEach((b) => {
    binario += String.fromCharCode(b);
  });
  return btoa(binario).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('inicia sem sessao ativa quando localStorage esta vazio', () => {
    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.isAdmin()).toBeFalse();
    expect(service.sessao()).toBeNull();
  });

  it('decodifica claims JWT em Base64URL com acentuacao UTF-8 sem corromper caracteres', () => {
    const header = paraBase64UrlUtf8({ alg: 'HS256', typ: 'JWT' });
    const payload = paraBase64UrlUtf8({
      sub: 'joao@gamb.com',
      nome: 'João São Paulo',
      sessaoWhatsapp: 'sessao_joao',
      exp: Math.floor(Date.now() / 1000) + 3600
    });
    const fakeJwt = `${header}.${payload}.assinatura`;

    const loginResponse: LoginResponse = {
      token: fakeJwt,
      email: 'joao@gamb.com',
      role: 'SUPER_ADMIN'
    };

    const credenciais: LoginRequest = { email: 'joao@gamb.com', senha: '123' };

    service.login(credenciais).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(loginResponse);

    expect(service.token()).toBe(fakeJwt);
    expect(service.sessao()?.nome).toBe('João São Paulo');
    expect(service.sessao()?.sessaoWhatsapp).toBe('sessao_joao');
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.isAdmin()).toBeTrue();
    expect(service.isSuperAdmin()).toBeTrue();
    expect(service.rotaInicial()).toBe('/admin');
    expect(service.possuiPapel('ADMIN')).toBeTrue();
  });

  it('token() retorna null e limpa localStorage quando a sessao esta expirada', () => {
    localStorage.setItem('gamb.jwt', 'token-expirado');
    localStorage.setItem('gamb.sessao', JSON.stringify({
      sub: 'exp@gamb.com',
      nome: 'Expirado',
      papel: 'ADMIN',
      exp: Date.now() - 1000
    }));
    service.sessao.set({
      sub: 'exp@gamb.com',
      nome: 'Expirado',
      papel: 'ADMIN',
      exp: Date.now() - 1000
    });

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token()).toBeNull();
    expect(localStorage.getItem('gamb.jwt')).toBeNull();
    expect(localStorage.getItem('gamb.sessao')).toBeNull();
    expect(service.sessao()).toBeNull();
  });

  it('identifica corretamente perfis ADMIN_OPERACIONAL, ADMIN_LEITURA e CLIENTE', () => {
    service.sessao.set({
      sub: 'op@gamb.com.br',
      nome: 'Operacional',
      papel: 'ADMIN_OPERACIONAL',
      exp: Date.now() + 60000
    });
    expect(service.isAdmin()).toBeTrue();
    expect(service.isSuperAdmin()).toBeFalse();
    expect(service.isLeitura()).toBeFalse();

    service.sessao.set({
      sub: 'leitor@gamb.com.br',
      nome: 'Leitor',
      papel: 'ADMIN_LEITURA',
      exp: Date.now() + 60000
    });
    expect(service.isLeitura()).toBeTrue();
    expect(service.isAdmin()).toBeTrue();
    expect(service.isSuperAdmin()).toBeFalse();

    service.sessao.set({
      sub: 'cliente@gamb.com.br',
      nome: 'Cliente',
      papel: 'CLIENTE',
      exp: Date.now() + 60000
    });
    expect(service.isAdmin()).toBeFalse();
    expect(service.possuiPapel('CLIENTE')).toBeTrue();
    expect(service.possuiPapel('ADMIN')).toBeFalse();
    expect(service.rotaInicial()).toBe('/cliente');
  });

  it('alterarSenha envia PUT com o payload esperado', () => {
    service.alterarSenha({ senhaAtual: '123', novaSenha: '456' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/alterar-senha`);
    expect(req.request.method).toBe('PUT');
    req.flush('');
  });

  it('logout limpa localStorage e redireciona para login', () => {
    localStorage.setItem('gamb.jwt', 'token-antigo');
    localStorage.setItem('gamb.sessao', '{"sub":"teste"}');

    service.logout();

    expect(localStorage.getItem('gamb.jwt')).toBeNull();
    expect(localStorage.getItem('gamb.sessao')).toBeNull();
    expect(service.sessao()).toBeNull();
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
