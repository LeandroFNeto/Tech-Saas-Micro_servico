import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER } from 'rxjs';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AuthService } from '../../../core/services/auth.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { LoginComponent } from './login.component';

describe('LoginComponent regressao visual', () => {
  const conversarPublico = jasmine.createSpy('conversarPublico').and.returnValue(NEVER);

  function montar() {
    const authStub = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(false),
      rotaInicial: jasmine.createSpy('rotaInicial').and.returnValue('/admin'),
      login: jasmine.createSpy('login')
    };
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        { provide: AssistenteTutoriaService, useValue: { conversarPublico } }
      ]
    });
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  it('chat do login abre com saudacao e sugestoes de acesso e envia origem LOGIN', () => {
    const fixture = montar();
    const raiz: HTMLElement = fixture.nativeElement;

    raiz.querySelector<HTMLButtonElement>('app-chat-flutuante button')?.click();
    fixture.detectChanges();

    const texto = raiz.querySelector('[role="dialog"]')?.textContent ?? '';
    expect(texto).toContain('Olá! Posso ajudar com o acesso ao painel');
    expect(texto).toContain('Como entro no painel?');
    expect(texto).toContain('Esqueci minha senha');
    expect(texto).not.toContain('QR Code');

    const sugestao = Array.from(raiz.querySelectorAll<HTMLButtonElement>('[role="dialog"] button'))
      .find(b => b.textContent?.includes('Quem é a Gamb?'));
    sugestao?.click();
    fixture.detectChanges();
    const enviar = Array.from(raiz.querySelectorAll<HTMLButtonElement>('[role="dialog"] button'))
      .find(b => b.textContent?.trim() === 'Enviar');
    enviar?.click();

    expect(conversarPublico).toHaveBeenCalledWith({ mensagem: 'Quem é a Gamb?', origem: 'LOGIN' });
    raiz.remove();
  });

  it('todo campo de formulario tem fonte computada de pelo menos 16px', () => {
    const fixture = montar();
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });
});
