import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER } from 'rxjs';
import { AssistenteTutoriaService } from '../../core/services/assistente-tutoria.service';
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
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        { provide: AssistenteTutoriaService, useValue: { conversarPublico: () => NEVER } }
      ]
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

  it('mostra o chat flutuante fechado com a dica e so o Suporte, sem Simulador', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;

    expect(raiz.querySelector('app-chat-flutuante')).not.toBeNull();
    expect(raiz.textContent).toContain('Para mais informações, fale com o chat agora');
    expect(raiz.querySelector('[role="dialog"]')).toBeNull();

    const botaoChat = raiz.querySelector<HTMLButtonElement>('app-chat-flutuante button');
    botaoChat?.click();
    fixture.detectChanges();

    expect(raiz.querySelector('[role="dialog"]')).not.toBeNull();
    expect(raiz.textContent).not.toContain('Simulador');
  });

  it('chat da apresentacao abre com sugestoes sobre a Gamb, sem QR Code nem Google Agenda', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;

    raiz.querySelector<HTMLButtonElement>('app-chat-flutuante button')?.click();
    fixture.detectChanges();

    const texto = raiz.querySelector('[role="dialog"]')?.textContent ?? '';
    expect(texto).toContain('Fale com a Gamb');
    expect(texto).toContain('Quem é a Gamb?');
    expect(texto).toContain('Como funciona o plano de assinatura?');
    expect(texto).not.toContain('QR Code');
    expect(texto).not.toContain('Google Agenda');
  });

  it('"Ver como funciona" leva ao id="como" e nao exibe [CONTATO DA GAMB]', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;
    const ancora = raiz.querySelector<HTMLAnchorElement>('a[href="#como"]');
    expect(ancora).withContext('link Ver como funciona').not.toBeNull();
    expect(raiz.querySelector('#como')).not.toBeNull();
    expect(raiz.innerHTML).not.toContain('[CONTATO DA GAMB]');
  });

  it('mostra Quem somos, Hoje e o que vem e Contato', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;
    const texto = raiz.textContent ?? '';

    expect(raiz.querySelector('#quem-somos')).not.toBeNull();
    expect(raiz.querySelector('#hoje')).not.toBeNull();
    expect(raiz.querySelector('#contato')).not.toBeNull();
    expect(texto).toContain('Funcionando');
    expect(texto).toContain('Em desenvolvimento');
    expect(texto).toContain('sem guardar informação de ninguém');
  });

  it('nao mostra preco nem nome de cliente', () => {
    const { fixture } = montar(false);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).not.toContain('R$');
  });

  it('links de WhatsApp abrem wa.me em nova aba e o numero aparece', () => {
    const { fixture } = montar(false);
    const raiz: HTMLElement = fixture.nativeElement;
    const links = Array.from(raiz.querySelectorAll<HTMLAnchorElement>('a'))
      .filter(a => a.getAttribute('href') === 'https://wa.me/5543998165047');

    expect(links.length).toBe(3);
    links.forEach(a => {
      expect(a.getAttribute('target')).toBe('_blank');
      expect(a.getAttribute('rel')).toContain('noopener');
    });
    expect(raiz.textContent).toContain('(43) 99816-5047');
  });
});
