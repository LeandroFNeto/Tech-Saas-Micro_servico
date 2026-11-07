import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../../core/services/auth.service';
import { PapelUsuario, SessaoUsuario } from '../../models/auth.models';

describe('ShellComponent', () => {
  let component: ShellComponent;
  let fixture: ComponentFixture<ShellComponent>;
  let sessaoSignal: ReturnType<typeof signal<SessaoUsuario | null>>;
  let authServiceStub: {
    sessao: typeof sessaoSignal;
    isAdmin: () => boolean;
    isLeitura: () => boolean;
    logout: jasmine.Spy;
  };

  function definirPapel(papel: PapelUsuario, nome = 'Usuário Teste', sub = 'user@gamb.com') {
    sessaoSignal.set({
      sub,
      nome,
      papel,
      exp: Date.now() + 3600000
    });
    fixture.detectChanges();
  }

  beforeEach(() => {
    sessaoSignal = signal<SessaoUsuario | null>(null);
    authServiceStub = {
      sessao: sessaoSignal,
      isAdmin: () => {
        const p = sessaoSignal()?.papel;
        return p === 'ADMIN' || p === 'SUPER_ADMIN' || p === 'ADMIN_OPERACIONAL' || p === 'ADMIN_LEITURA';
      },
      isLeitura: () => sessaoSignal()?.papel === 'ADMIN_LEITURA',
      logout: jasmine.createSpy('logout')
    };

    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceStub }
      ]
    });

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
  });

  it('exibe dados do usuario autenticado corretamente', () => {
    definirPapel('ADMIN', 'Administrador Chefe', 'admin@techsaas.com');

    expect(component.admin).toBeTrue();
    expect(component.nomeUsuario).toBe('Administrador Chefe');
    expect(component.emailUsuario).toBe('admin@techsaas.com');
  });

  const papeisAdminComCriacao: PapelUsuario[] = ['SUPER_ADMIN', 'ADMIN_OPERACIONAL'];
  papeisAdminComCriacao.forEach((papel) => {
    it(`exibe menu administrativo com botao Novo cliente e sem chat flutuante para ${papel}`, () => {
      definirPapel(papel);
      const raiz: HTMLElement = fixture.nativeElement;

      expect(component.admin).toBeTrue();
      expect(component.mostrarChat).toBeFalse();
      expect(raiz.querySelector('app-chat-flutuante')).toBeNull();
      expect(raiz.querySelectorAll('a[href="/admin/empresas/nova"]').length).toBeGreaterThan(0);
      expect(raiz.textContent).toContain('Empresas');
      expect(raiz.textContent).toContain('Novo cliente');
    });
  });

  it('exibe menu administrativo sem botao Novo cliente e sem chat flutuante para ADMIN_LEITURA', () => {
    definirPapel('ADMIN_LEITURA', 'Auditor', 'leitura@gamb.com');
    const raiz: HTMLElement = fixture.nativeElement;

    expect(component.admin).toBeTrue();
    expect(component.mostrarChat).toBeFalse();
    expect(raiz.querySelector('app-chat-flutuante')).toBeNull();
    expect(raiz.querySelector('a[href="/admin/empresas/nova"]')).toBeNull();
    expect(raiz.textContent).toContain('Empresas');
    expect(raiz.textContent).not.toContain('Novo cliente');
  });

  it('identifica usuario cliente como nao admin e mostra menu do cliente', () => {
    definirPapel('CLIENTE', 'Cliente Usuario', 'cliente@techsaas.com');
    const raiz: HTMLElement = fixture.nativeElement;

    expect(component.admin).toBeFalse();
    expect(component.mostrarChat).toBeTrue();
    expect(raiz.textContent).toContain('Tutoriais');
  });

  it('chama logout ao acionar sair()', () => {
    component.sair();
    expect(authServiceStub.logout).toHaveBeenCalled();
  });

  it('nao mostra o chat flutuante na tela de Tutoriais', () => {
    definirPapel('CLIENTE', 'Cliente', 'c@x.com');
    spyOnProperty(TestBed.inject(Router), 'url').and.returnValue('/cliente/tutoriais');

    expect(component.mostrarChat).toBeFalse();
  });
});
