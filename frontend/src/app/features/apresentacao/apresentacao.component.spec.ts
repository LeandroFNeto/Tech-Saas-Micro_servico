import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ApresentacaoComponent } from './apresentacao.component';

describe('ApresentacaoComponent', () => {
  let authStub: { isAuthenticated: jasmine.Spy; rotaInicial: jasmine.Spy };

  function montar(autenticado: boolean) {
    authStub = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(autenticado),
      rotaInicial: jasmine.createSpy('rotaInicial').and.returnValue('/admin/empresas')
    };
    TestBed.configureTestingModule({
      imports: [ApresentacaoComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub }]
    });
    const router = TestBed.inject(Router);
    const navegar = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    const fixture = TestBed.createComponent(ApresentacaoComponent);
    fixture.detectChanges();
    return { fixture, navegar };
  }

  it('sem sessao renderiza e os links Entrar (topo e fim) apontam para /login', () => {
    const { fixture, navegar } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;
    const links = Array.from(raiz.querySelectorAll<HTMLAnchorElement>('a'))
      .filter(a => a.getAttribute('href') === '/login');
    expect(links.length).toBe(2);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('com sessao valida navega para a rota inicial do papel', () => {
    const { navegar } = montar(true);
    expect(authStub.rotaInicial).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith('/admin/empresas');
  });

  it('"Ver como funciona" leva ao id="como" e nao exibe [CONTATO DA GAMB]', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;
    const ancora = raiz.querySelector<HTMLAnchorElement>('a[href="#como"]');
    expect(ancora).withContext('link Ver como funciona').not.toBeNull();
    expect(raiz.querySelector('#como')).not.toBeNull();
    expect(raiz.innerHTML).not.toContain('[CONTATO DA GAMB]');
  });
});
