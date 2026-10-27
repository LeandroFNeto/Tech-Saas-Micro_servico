import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { LoginComponent } from './login.component';

describe('LoginComponent regressao visual', () => {
  function montar() {
    const authStub = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(false),
      rotaInicial: jasmine.createSpy('rotaInicial').and.returnValue('/admin'),
      login: jasmine.createSpy('login')
    };
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub }]
    });
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  it('todo campo de formulario tem fonte computada de pelo menos 16px', () => {
    const fixture = montar();
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });
});
