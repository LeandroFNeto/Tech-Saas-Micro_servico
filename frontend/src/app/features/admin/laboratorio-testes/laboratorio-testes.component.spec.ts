import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { DiagnosticoResponseDTO } from '../../../shared/models/assistente.models';
import { LaboratorioTestesComponent } from './laboratorio-testes.component';

describe('LaboratorioTestesComponent', () => {
  let component: LaboratorioTestesComponent;
  let fixture: ComponentFixture<LaboratorioTestesComponent>;
  let assistenteService: jasmine.SpyObj<AssistenteTutoriaService>;

  const mockDiagnostico: DiagnosticoResponseDTO = {
    statusGeral: 'OPERACIONAL',
    dataHora: '2026-11-03T14:30:00Z',
    modulos: [
      { modulo: 'Auth', status: 'OPERACIONAL', mensagem: 'OK', tempoRespostaMs: 12 },
      { modulo: 'Avisos', status: 'OPERACIONAL', mensagem: 'OK', tempoRespostaMs: 14 }
    ]
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('AssistenteTutoriaService', ['executarDiagnostico', 'conversar']);

    await TestBed.configureTestingModule({
      imports: [LaboratorioTestesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AssistenteTutoriaService, useValue: spy }
      ]
    }).compileComponents();

    assistenteService = TestBed.inject(AssistenteTutoriaService) as jasmine.SpyObj<AssistenteTutoriaService>;
    assistenteService.executarDiagnostico.and.returnValue(of(mockDiagnostico));
    assistenteService.conversar.and.returnValue(
      of({
        resposta: 'Instruções da IA',
        modo: 'DIAGNOSTICO',
        dataHora: '2026-11-03T14:30:05Z',
        sugestoes: ['Sugestão Nova']
      })
    );

    fixture = TestBed.createComponent(LaboratorioTestesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve ser instanciado e carregar diagnostico no ngOnInit', () => {
    expect(component).toBeTruthy();
    expect(assistenteService.executarDiagnostico).toHaveBeenCalled();
    expect(component.diagnostico()).toEqual(mockDiagnostico);
  });

  it('deve alternar entre Modo Diagnostico e Atendimento Sandbox', () => {
    expect(component.modo()).toBe('DIAGNOSTICO');
    component.alternarModo('ATENDIMENTO_SANDBOX');
    expect(component.modo()).toBe('ATENDIMENTO_SANDBOX');
    expect(component.sugestoes()).toContain('Quais são os horários e regras de locação?');
  });

  it('deve enviar mensagem para a IA e atualizar historico', () => {
    component.mensagemInput = 'Como conectar o WhatsApp?';
    component.enviarMensagem();

    expect(assistenteService.conversar).toHaveBeenCalledWith({
      mensagem: 'Como conectar o WhatsApp?',
      modo: 'DIAGNOSTICO'
    });
    expect(component.mensagens().length).toBeGreaterThan(1);
    expect(component.mensagens().some((m) => m.texto === 'Instruções da IA')).toBeTrue();
  });
});
