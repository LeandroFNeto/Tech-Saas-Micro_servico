import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AutomacaoService, ContatoResumoDTO, ReservaResumoDTO } from '../../../core/services/automacao.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { AutomacaoComponent } from './automacao.component';

describe('AutomacaoComponent', () => {
  let component: AutomacaoComponent;
  let fixture: ComponentFixture<AutomacaoComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let automacaoServiceSpy: jasmine.SpyObj<AutomacaoService>;
  let empresaServiceSpy: jasmine.SpyObj<EmpresaService>;

  const reservaMock: ReservaResumoDTO = {
    id: 15,
    data: '2026-11-20',
    horario: '14:00',
    nomeCliente: 'João Reserva',
    telefoneMascarado: '(11) *****-4321',
    situacao: 'PRONTA_PARA_AVISAR',
    podeAvisar: true,
    contatoId: null
  };

  const contatoMock: ContatoResumoDTO = {
    id: 9,
    nome: 'Maria Contato',
    telefoneMascarado: '(11) *****-8888',
    primeiraConversaEm: '2026-11-01',
    ultimaConversaEm: '2026-11-05',
    origem: 'CONVERSA'
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['sessao']);
    authServiceSpy.sessao.and.returnValue({
      sub: 'empresa1',
      nome: 'Empresa 1',
      papel: 'CLIENTE',
      sessaoWhatsapp: 'sessao-empresa-1',
      exp: Date.now() + 3600000
    });

    automacaoServiceSpy = jasmine.createSpyObj('AutomacaoService', [
      'listarReservas',
      'enviarLembrete',
      'vincularContato',
      'listarContatos',
      'obterNumeroContato'
    ]);
    automacaoServiceSpy.listarReservas.and.returnValue(of([reservaMock]));
    automacaoServiceSpy.listarContatos.and.returnValue(of([contatoMock]));

    empresaServiceSpy = jasmine.createSpyObj('EmpresaService', [
      'buscarPorSessao',
      'atualizarPeloCliente'
    ]);
    empresaServiceSpy.buscarPorSessao.and.returnValue(of({
      id: 1,
      nome: 'Empresa Teste',
      sessaoWhatsapp: 'sessao-empresa-1',
      modulosAtivos: [],
      modulosMenu: [],
      lembreteConfirmacao: {
        modo: 'SUGERIDO',
        diasAntes: 2,
        horarioEnvio: '11:00',
        texto: 'Texto teste'
      }
    }));

    await TestBed.configureTestingModule({
      imports: [AutomacaoComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AutomacaoService, useValue: automacaoServiceSpy },
        { provide: EmpresaService, useValue: empresaServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AutomacaoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar carregando lembreteConfirmacao da empresa', () => {
    expect(component.lembreteModo()).toBe('SUGERIDO');
    expect(component.lembreteDiasAntes()).toBe(2);
    expect(component.lembreteHorarioEnvio()).toBe('11:00');
    expect(component.lembreteTexto()).toBe('Texto teste');
  });

  it('exibe erro visivel quando carregarDadosEmpresa falha', () => {
    empresaServiceSpy.buscarPorSessao.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Falha ao ler empresa' } }))
    );

    component.carregarDadosEmpresa();
    fixture.detectChanges();

    expect(component.lembreteMensagemErro()).toBe('Falha ao ler empresa');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Falha ao ler empresa');
  });

  it('garante fonte de pelo menos 16px, labels associados e maxlength nos campos do lembrete e modal', () => {
    const raiz: HTMLElement = fixture.nativeElement;
    document.body.appendChild(raiz);
    garantirTailwindCarregado(raiz);

    expect(medirCampos(raiz).length).toBe(4);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    expect(raiz.querySelector('textarea#lembrete-texto')?.getAttribute('maxlength')).toBe('1000');
    expect(raiz.querySelector('label[for="lembrete-modo"]')).not.toBeNull();

    component.abrirModalContato(reservaMock);
    fixture.detectChanges();

    const modal = raiz.querySelector('[role="dialog"]');
    expect(modal?.getAttribute('aria-modal')).toBe('true');
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(component.modalContatoAberto()).toBeFalse();

    raiz.remove();
  });

  it('salvarLembrete valida formato HH:mm, envia lembreteConfirmacao no sucesso e trata erro HTTP', () => {
    component.lembreteHorarioEnvio.set('25:99');
    component.salvarLembrete();
    expect(empresaServiceSpy.atualizarPeloCliente).not.toHaveBeenCalled();
    expect(component.lembreteMensagemErro()).toContain('HH:mm');

    empresaServiceSpy.atualizarPeloCliente.and.returnValue(of({} as any));
    component.lembreteHorarioEnvio.set('09:30');
    component.lembreteTexto.set('Confirme sua presença');
    component.salvarLembrete();

    expect(empresaServiceSpy.atualizarPeloCliente).toHaveBeenCalledWith('sessao-empresa-1', {
      lembreteConfirmacao: {
        modo: 'SUGERIDO',
        diasAntes: 2,
        horarioEnvio: '09:30',
        texto: 'Confirme sua presença'
      }
    });
    expect(component.lembreteMensagemSucesso()).toBeTruthy();

    empresaServiceSpy.atualizarPeloCliente.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    component.salvarLembrete();
    expect(component.lembreteMensagemErro()).toBe(component.textos.lembrete.botoes.erro);
    expect(component.lembreteSalvando()).toBeFalse();
  });

  it('avisarAgora recarrega reservas no sucesso e exibe erro HTTP na falha', () => {
    component.mudarAba('reservas');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('(11) *****-4321');

    automacaoServiceSpy.enviarLembrete.and.returnValue(of(undefined));
    automacaoServiceSpy.listarReservas.calls.reset();
    component.avisarAgora(reservaMock);

    expect(automacaoServiceSpy.enviarLembrete).toHaveBeenCalledWith('sessao-empresa-1', 15);
    expect(automacaoServiceSpy.listarReservas).toHaveBeenCalledWith('sessao-empresa-1');
    expect(component.enviandoLembreteId()).toBeNull();

    automacaoServiceSpy.enviarLembrete.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 503, error: { message: 'WhatsApp desconectado' } }))
    );
    component.avisarAgora(reservaMock);
    expect(component.enviandoLembreteId()).toBeNull();
    expect(component.erroReservas()).toBe('WhatsApp desconectado');
  });

  it('confirmarVinculoContato bloqueia vazio/invalido, envia campo telefone no sucesso e trata erro HTTP', () => {
    component.abrirModalContato(reservaMock);

    component.confirmarVinculoContato();
    expect(automacaoServiceSpy.vincularContato).not.toHaveBeenCalled();
    expect(component.erroModalContato()).toContain('Selecione um contato');

    component.novoTelefone.set('123');
    component.confirmarVinculoContato();
    expect(automacaoServiceSpy.vincularContato).not.toHaveBeenCalled();
    expect(component.erroModalContato()).toContain('DDD');

    automacaoServiceSpy.vincularContato.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'Telefone inválido no servidor' } }))
    );
    component.novoTelefone.set('11987654321');
    component.confirmarVinculoContato();
    expect(component.erroModalContato()).toBe('Telefone inválido no servidor');
    expect(component.modalContatoAberto()).toBeTrue();

    automacaoServiceSpy.vincularContato.and.returnValue(of(undefined));
    component.confirmarVinculoContato();
    expect(automacaoServiceSpy.vincularContato).toHaveBeenCalledWith('sessao-empresa-1', 15, {
      contatoId: undefined,
      telefone: '11987654321'
    });
    expect(component.modalContatoAberto()).toBeFalse();
  });

  it('revelarNumero trata sucesso 200, limite 429 e erro 500 na aba contatos', () => {
    component.mudarAba('contatos');
    fixture.detectChanges();
    expect(component.obterTelefoneExibicao(contatoMock)).toBe('(11) *****-8888');

    automacaoServiceSpy.obterNumeroContato.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 429 }))
    );
    component.revelarNumero(9);
    expect(component.avisoLimite()).toBe(component.textos.contatos.avisoLimite);

    automacaoServiceSpy.obterNumeroContato.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Erro de auditoria' } }))
    );
    component.revelarNumero(9);
    expect(component.avisoLimite()).toBeNull();
    expect(component.erroContatos()).toBe('Erro de auditoria');

    automacaoServiceSpy.obterNumeroContato.and.returnValue(of({ numero: '5511999998888' }));
    component.revelarNumero(9);
    expect(component.obterTelefoneExibicao(contatoMock)).toBe('5511999998888');
  });
});
