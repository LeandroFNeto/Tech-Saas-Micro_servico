import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { EmpresaFormComponent } from './empresa-form.component';

const mockAuth = {
  isAdmin: () => true,
  isSuperAdmin: () => true,
  isLeitura: () => false,
  possuiPapel: () => true
};

describe('EmpresaFormComponent regressao visual', () => {
  const empresa = {
    nome: 'Quadra Teste',
    sessaoWhatsapp: 'quadra',
    ramoDeAtuacao: 'locacao',
    locacaoPorHora: true,
    googleCalendarId: 'agenda@group.calendar.google.com',
    linkFotoPrincipal: 'https://exemplo.com/a.jpg',
    urlsGaleria: ['https://exemplo.com/b.jpg'],
    linkGaleria: '',
    modulosAtivos: []
  };

  function montar(sessao: string) {
    TestBed.configureTestingModule({
      imports: [EmpresaFormComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ sessao }) } } },
        { provide: EmpresaService, useValue: { buscarPorSessao: jasmine.createSpy('buscarPorSessao').and.returnValue(of(empresa)) } },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => NEVER, iniciar: () => NEVER } },
        { provide: AuthService, useValue: mockAuth }
      ]
    });
    const fixture = TestBed.createComponent(EmpresaFormComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  it('cadastro novo: todo campo tem fonte computada de pelo menos 16px', () => {
    const fixture = montar('nova');
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });

  it('edicao: Sessao WhatsApp e ID da agenda Google tem pelo menos 16px', () => {
    const fixture = montar('quadra');
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    const campos = medirCampos(raiz).map((campo) => campo.descricao);
    expect(campos.some((descricao) => descricao.includes('sessaoWhatsapp'))).toBeTrue();
    expect(campos.some((descricao) => descricao.includes('googleCalendarId'))).toBeTrue();
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });
});

describe('EmpresaFormComponent, reserva automatica pelo bot', () => {
  const MENSAGEM_AGENDA = 'Cadastre a agenda do Google antes de ligar a reserva automática.';

  function montarComApi(sessao: string, api: Record<string, unknown>) {
    TestBed.configureTestingModule({
      imports: [EmpresaFormComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ sessao }) } } },
        {
          provide: EmpresaService,
          useValue: {
            buscarPorSessao: () =>
              of({
                nome: 'Quadra Teste',
                sessaoWhatsapp: 'quadra',
                ramoDeAtuacao: 'locacao',
                googleCalendarId: 'agenda@group.calendar.google.com',
                urlsGaleria: [],
                modulosAtivos: []
              }),
            ...api
          }
        },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => NEVER, iniciar: () => NEVER } },
        { provide: AuthService, useValue: mockAuth }
      ]
    });
    const fixture = TestBed.createComponent(EmpresaFormComponent);
    fixture.detectChanges();
    return fixture;
  }

  function erro400(mensagem: string) {
    return new HttpErrorResponse({ status: 400, error: { message: mensagem } });
  }

  function preencherCadastro(fixture: ReturnType<typeof montarComApi>, ligada: boolean) {
    fixture.componentInstance.form.patchValue({
      nome: 'Espaço Novo',
      sessaoWhatsapp: 'espaco_novo',
      senha: 'SenhaCliente123',
      permiteReservaAutomatica: ligada
    });
  }

  it('cadastro novo envia o controle ligado no POST', () => {
    const criar = jasmine.createSpy('criar').and.returnValue(of({}));
    const fixture = montarComApi('nova', { criar });
    preencherCadastro(fixture, true);
    fixture.componentInstance.form.patchValue({ googleCalendarId: 'agenda@group.calendar.google.com' });

    fixture.componentInstance.salvar();

    expect(criar).toHaveBeenCalledTimes(1);
    expect(criar.calls.mostRecent().args[0].permiteReservaAutomatica).toBeTrue();
  });

  it('cadastro novo envia false quando o interruptor fica desligado', () => {
    const criar = jasmine.createSpy('criar').and.returnValue(of({}));
    const fixture = montarComApi('nova', { criar });
    preencherCadastro(fixture, false);

    fixture.componentInstance.salvar();

    expect(criar.calls.mostRecent().args[0].permiteReservaAutomatica).toBeFalse();
  });

  it('edicao envia o controle no PUT do admin', () => {
    const atualizarPeloAdmin = jasmine.createSpy('atualizarPeloAdmin').and.returnValue(of({}));
    const fixture = montarComApi('quadra', { atualizarPeloAdmin });
    fixture.componentInstance.form.patchValue({ permiteReservaAutomatica: true });

    fixture.componentInstance.salvar();

    expect(atualizarPeloAdmin).toHaveBeenCalledTimes(1);
    const [sessao, dto] = atualizarPeloAdmin.calls.mostRecent().args;
    expect(sessao).toBe('quadra');
    expect(dto.permiteReservaAutomatica).toBeTrue();
  });

  it('400 que cita a agenda acende o erro da linha e mostra o aviso do topo', () => {
    const atualizarPeloAdmin = jasmine
      .createSpy('atualizarPeloAdmin')
      .and.returnValue(throwError(() => erro400(MENSAGEM_AGENDA)));
    const fixture = montarComApi('quadra', { atualizarPeloAdmin });
    fixture.componentInstance.form.patchValue({ permiteReservaAutomatica: true, googleCalendarId: '' });

    fixture.componentInstance.salvar();
    fixture.detectChanges();

    expect(fixture.componentInstance.erro()).toBe(MENSAGEM_AGENDA);
    expect(fixture.componentInstance.erroReservaAutomatica()).toBe(MENSAGEM_AGENDA);
    expect(fixture.componentInstance.salvando()).toBeFalse();
    const raiz: HTMLElement = fixture.nativeElement;
    expect(raiz.querySelector('#empresa-reserva-erro')?.textContent).toContain(MENSAGEM_AGENDA);
    expect(raiz.querySelector('input[formControlName="permiteReservaAutomatica"]')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('400 do POST que cita a agenda tambem acende o erro da linha', () => {
    const criar = jasmine.createSpy('criar').and.returnValue(throwError(() => erro400(MENSAGEM_AGENDA)));
    const fixture = montarComApi('nova', { criar });
    preencherCadastro(fixture, true);

    fixture.componentInstance.salvar();

    expect(fixture.componentInstance.erroReservaAutomatica()).toBe(MENSAGEM_AGENDA);
    expect(fixture.componentInstance.salvando()).toBeFalse();
  });

  it('400 que nao cita a agenda nao acende o erro da linha', () => {
    const atualizarPeloAdmin = jasmine
      .createSpy('atualizarPeloAdmin')
      .and.returnValue(throwError(() => erro400('Sessão WhatsApp já ocupada.')));
    const fixture = montarComApi('quadra', { atualizarPeloAdmin });

    fixture.componentInstance.salvar();

    expect(fixture.componentInstance.erro()).toBe('Sessão WhatsApp já ocupada.');
    expect(fixture.componentInstance.erroReservaAutomatica()).toBe('');
    expect(fixture.componentInstance.salvando()).toBeFalse();
  });

  it('erro 500 que cita a agenda nao acende o erro da linha', () => {
    const atualizarPeloAdmin = jasmine
      .createSpy('atualizarPeloAdmin')
      .and.returnValue(throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'agenda fora do ar' } })));
    const fixture = montarComApi('quadra', { atualizarPeloAdmin });

    fixture.componentInstance.salvar();

    expect(fixture.componentInstance.erroReservaAutomatica()).toBe('');
  });

  it('salvar de novo limpa o erro da linha', () => {
    const atualizarPeloAdmin = jasmine
      .createSpy('atualizarPeloAdmin')
      .and.returnValues(throwError(() => erro400(MENSAGEM_AGENDA)), of({}));
    const fixture = montarComApi('quadra', { atualizarPeloAdmin });

    fixture.componentInstance.salvar();
    expect(fixture.componentInstance.erroReservaAutomatica()).not.toBe('');
    fixture.componentInstance.salvar();

    expect(fixture.componentInstance.erroReservaAutomatica()).toBe('');
  });
});
