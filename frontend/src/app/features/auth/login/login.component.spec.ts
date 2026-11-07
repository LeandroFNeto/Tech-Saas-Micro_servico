import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AuthService } from '../../../core/services/auth.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let conversarPublico: jasmine.Spy;
  let authStub: {
    isAuthenticated: jasmine.Spy;
    rotaInicial: jasmine.Spy;
    login: jasmine.Spy;
  };

  function montar(autenticado = false, rotaInicial = '/admin') {
    conversarPublico = jasmine.createSpy('conversarPublico').and.returnValue(NEVER);
    authStub = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(autenticado),
      rotaInicial: jasmine.createSpy('rotaInicial').and.returnValue(rotaInicial),
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
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return { fixture, router };
  }

  it('chat do login abre com saudacao e sugestoes de acesso e envia origem LOGIN', () => {
    const { fixture } = montar();
    const raiz: HTMLElement = fixture.nativeElement;
    document.body.appendChild(raiz);

    raiz.querySelector<HTMLButtonElement>('app-chat-flutuante button')?.click();
    fixture.detectChanges();

    const texto = raiz.querySelector('[role="dialog"]')?.textContent ?? '';
    expect(texto).toContain('Olá! Posso ajudar com o acesso ao painel');
    expect(texto).toContain('Como entro no painel?');
    expect(texto).toContain('Esqueci minha senha');
    expect(texto).not.toContain('QR Code');

    const sugestao = Array.from(raiz.querySelectorAll<HTMLButtonElement>('[role="dialog"] button'))
      .find((b) => b.textContent?.includes('Quem é a Gamb?'));
    sugestao?.click();
    fixture.detectChanges();
    const enviar = Array.from(raiz.querySelectorAll<HTMLButtonElement>('[role="dialog"] button'))
      .find((b) => b.textContent?.trim() === 'Enviar');
    enviar?.click();

    expect(conversarPublico).toHaveBeenCalledWith({ mensagem: 'Quem é a Gamb?', origem: 'LOGIN' });
    raiz.remove();
  });

  it('todo campo de formulario tem fonte computada de pelo menos 16px', () => {
    const { fixture } = montar();
    const raiz: HTMLElement = fixture.nativeElement;
    document.body.appendChild(raiz);
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });

  it('redireciona imediatamente no construtor se ja estiver autenticado', () => {
    const { router } = montar(true, '/cliente');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/cliente');
  });

  it('aceita usuario@gamb.com, sessao@gamb.com.br, maiusculas e espacos nas bordas fazendo trim', () => {
    const { fixture, router } = montar(false, '/admin');
    const comp = fixture.componentInstance;
    authStub.login.and.returnValue(of({ token: 'jwt', email: 'usuario@gamb.com', role: 'SUPER_ADMIN' }));

    comp.form.setValue({ email: '  USUARIO@GAMB.COM  ', senha: 'SenhaMaster123' });
    expect(comp.form.valid).toBeTrue();
    comp.entrar();

    expect(authStub.login).toHaveBeenCalledWith({ email: 'USUARIO@GAMB.COM', senha: 'SenhaMaster123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');

    authStub.rotaInicial.and.returnValue('/cliente');
    comp.form.setValue({ email: 'sessao@gamb.com.br', senha: 'SenhaCliente123' });
    comp.entrar();
    expect(authStub.login).toHaveBeenCalledWith({ email: 'sessao@gamb.com.br', senha: 'SenhaCliente123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/cliente');
  });

  it('redireciona ADMIN_LEITURA para /admin apos login', () => {
    const { fixture, router } = montar(false, '/admin');
    authStub.login.and.returnValue(of({ token: 'jwt', email: 'leitor@gamb.com', role: 'ADMIN_LEITURA' }));

    fixture.componentInstance.form.setValue({ email: 'leitor@gamb.com', senha: 'SenhaLeitor123' });
    fixture.componentInstance.entrar();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');
  });

  it('bloqueia envio quando e-mail ou senha sao invalidos', () => {
    const { fixture } = montar();
    const comp = fixture.componentInstance;

    comp.form.setValue({ email: 'invalido-sem-dominio', senha: '123' });
    comp.entrar();

    expect(authStub.login).not.toHaveBeenCalled();
    expect(comp.emailInvalido).toBeTrue();
  });

  it('trata erros HTTP 401, 500 e 0 ao tentar autenticar', () => {
    const { fixture } = montar();
    const comp = fixture.componentInstance;
    comp.form.setValue({ email: 'usuario@gamb.com', senha: 'SenhaErrada123' });

    authStub.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    comp.entrar();
    expect(comp.erro).toBe(comp.conteudo.erros.credenciaisInvalidas);
    expect(comp.enviando).toBeFalse();

    authStub.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    comp.entrar();
    expect(comp.erro).toContain('500');

    authStub.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    comp.entrar();
    expect(comp.erro).toContain('indisponível');
    expect(comp.erro).not.toContain('localhost:8080');
  });

  it('alternarSenha alterna exibicao do campo de senha', () => {
    const { fixture } = montar();
    const comp = fixture.componentInstance;
    const campo = () => (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#login-senha')!;

    expect(comp.mostrarSenha()).toBeFalse();
    expect(campo().type).toBe('password');

    comp.alternarSenha();
    fixture.detectChanges();
    expect(comp.mostrarSenha()).toBeTrue();
    expect(campo().type).toBe('text');
  });
});
