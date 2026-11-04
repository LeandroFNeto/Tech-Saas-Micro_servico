import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'possuiPapel',
      'isAuthenticated',
      'rotaInicial'
    ]);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('permite acesso quando usuario possui o papel esperado', () => {
    authServiceSpy.possuiPapel.and.returnValue(true);

    const guard = roleGuard('ADMIN');
    const resultado = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(resultado).toBeTrue();
    expect(authServiceSpy.possuiPapel).toHaveBeenCalledWith('ADMIN');
  });

  it('redireciona para rota inicial quando autenticado porem sem o papel', () => {
    authServiceSpy.possuiPapel.and.returnValue(false);
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.rotaInicial.and.returnValue('/cliente');

    const guard = roleGuard('ADMIN');
    const resultado = TestBed.runInInjectionContext(() => guard({} as any, {} as any)) as UrlTree;

    expect(resultado).toEqual(router.createUrlTree(['/cliente']));
  });

  it('redireciona para login quando nao possui o papel e nao esta autenticado', () => {
    authServiceSpy.possuiPapel.and.returnValue(false);
    authServiceSpy.isAuthenticated.and.returnValue(false);

    const guard = roleGuard('ADMIN');
    const resultado = TestBed.runInInjectionContext(() => guard({} as any, {} as any)) as UrlTree;

    expect(resultado).toEqual(router.createUrlTree(['/login']));
  });
});
