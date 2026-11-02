import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.models';

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

  it('deve realizar login, persistir sessao e decodificar token corretamente', () => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: 'admin@gamb.com.br',
      nome: 'Administrador',
      sessaoWhatsapp: 'sessao-1',
      exp: Math.floor(Date.now() / 1000) + 3600
    }));
    const fakeJwt = `${header}.${payload}.assinatura`;

    const loginResponse: LoginResponse = {
      token: fakeJwt,
      email: 'admin@gamb.com.br',
      role: 'ADMIN',
      nome: 'Administrador',
      sessaoWhatsapp: 'sessao-1'
    };

    const credenciais: LoginRequest = { email: 'admin@gamb.com.br', senha: '123' };

    service.login(credenciais).subscribe((res) => {
      expect(res.token).toBe(fakeJwt);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(loginResponse);

    expect(service.token()).toBe(fakeJwt);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.isAdmin()).toBeTrue();
    expect(service.isSuperAdmin()).toBeTrue();
    expect(service.rotaInicial()).toBe('/admin');
    expect(service.possuiPapel('ADMIN')).toBeTrue();
  });

  it('identifica corretamente perfis de cliente e leitura', () => {
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
