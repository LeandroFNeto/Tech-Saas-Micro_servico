import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('permite acesso quando usuario esta autenticado', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    const resultado = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(resultado).toBeTrue();
  });

  it('redireciona para login quando usuario nao esta autenticado', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    const resultado = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any)) as UrlTree;

    expect(resultado).toEqual(router.createUrlTree(['/login']));
  });
});
