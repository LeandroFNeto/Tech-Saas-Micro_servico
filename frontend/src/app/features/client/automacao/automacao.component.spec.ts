import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AutomacaoService } from '../../../core/services/automacao.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { AutomacaoComponent } from './automacao.component';

describe('AutomacaoComponent', () => {
  let component: AutomacaoComponent;
  let fixture: ComponentFixture<AutomacaoComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let automacaoServiceSpy: jasmine.SpyObj<AutomacaoService>;
  let empresaServiceSpy: jasmine.SpyObj<EmpresaService>;

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
    automacaoServiceSpy.listarReservas.and.returnValue(of([]));
    automacaoServiceSpy.listarContatos.and.returnValue(of([]));

    empresaServiceSpy = jasmine.createSpyObj('EmpresaService', [
      'buscarPorSessao',
      'atualizarPeloCliente'
    ]);
    empresaServiceSpy.buscarPorSessao.and.returnValue(of({
      id: 1,
      nome: 'Empresa Teste',
      sessaoWhatsapp: 'sessao-empresa-1',
      lembrete: {
        modo: 'SUGERIDO',
        diasAntes: 2,
        horarioEnvio: '11:00',
        texto: 'Texto teste'
      }
    } as any));

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

  it('deve inicializar carregando dados do lembrete da empresa', () => {
    expect(component).toBeTruthy();
    expect(component.lembreteModo()).toBe('SUGERIDO');
    expect(component.lembreteDiasAntes()).toBe(2);
    expect(component.lembreteHorarioEnvio()).toBe('11:00');
  });

  it('deve trocar de aba para reservas e listar dados', () => {
    component.mudarAba('reservas');
    expect(component.abaAtiva()).toBe('reservas');
    expect(automacaoServiceSpy.listarReservas).toHaveBeenCalledWith('sessao-empresa-1');
  });

  it('deve trocar de aba para contatos e listar dados', () => {
    component.mudarAba('contatos');
    expect(component.abaAtiva()).toBe('contatos');
    expect(automacaoServiceSpy.listarContatos).toHaveBeenCalledWith('sessao-empresa-1');
  });
});
