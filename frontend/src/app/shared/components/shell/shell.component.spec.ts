import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../../core/services/auth.service';
import { SessaoUsuario } from '../../models/auth.models';

describe('ShellComponent', () => {
  let component: ShellComponent;
  let fixture: ComponentFixture<ShellComponent>;
  let sessaoSignal: ReturnType<typeof signal<SessaoUsuario | null>>;
  let authServiceSpy: { sessao: typeof sessaoSignal; logout: jasmine.Spy };

  beforeEach(() => {
    sessaoSignal = signal<SessaoUsuario | null>(null);
    authServiceSpy = {
      sessao: sessaoSignal,
      logout: jasmine.createSpy('logout')
    };

    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
  });

  it('exibe dados do usuario autenticado corretamente', () => {
    sessaoSignal.set({
      sub: 'admin@techsaas.com',
      nome: 'Administrador Chefe',
      papel: 'ADMIN',
      exp: Date.now() + 3600000
    });

    expect(component.admin).toBeTrue();
    expect(component.nomeUsuario).toBe('Administrador Chefe');
    expect(component.emailUsuario).toBe('admin@techsaas.com');
  });

  it('identifica usuario cliente como nao admin', () => {
    sessaoSignal.set({
      sub: 'cliente@techsaas.com',
      nome: 'Cliente Usuario',
      papel: 'CLIENTE',
      exp: Date.now() + 3600000
    });

    expect(component.admin).toBeFalse();
  });

  it('chama logout ao acionar sair()', () => {
    component.sair();
    expect(authServiceSpy.logout).toHaveBeenCalled();
  });

  it('expoe conteudo do menu cliente incluindo tutoriais', () => {
    expect(component.conteudo.menuCliente.tutoriais).toBe('Tutoriais');
  });

  it('mostra o chat flutuante para o cliente fora de Tutoriais', () => {
    sessaoSignal.set({ sub: 'c@x.com', nome: 'Cliente', papel: 'CLIENTE', exp: Date.now() + 3600000 });

    expect(component.mostrarChat).toBeTrue();
  });

  it('nao mostra o chat flutuante na tela de Tutoriais', async () => {
    sessaoSignal.set({ sub: 'c@x.com', nome: 'Cliente', papel: 'CLIENTE', exp: Date.now() + 3600000 });
    spyOnProperty(TestBed.inject(Router), 'url').and.returnValue('/cliente/tutoriais');

    expect(component.mostrarChat).toBeFalse();
  });

  it('nao mostra o chat flutuante na area do admin', () => {
    sessaoSignal.set({ sub: 'a@x.com', nome: 'Admin', papel: 'ADMIN', exp: Date.now() + 3600000 });

    expect(component.mostrarChat).toBeFalse();
  });
});
