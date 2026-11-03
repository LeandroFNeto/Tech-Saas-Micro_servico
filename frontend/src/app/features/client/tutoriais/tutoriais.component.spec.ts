import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TutoriaisComponent } from './tutoriais.component';
import { AvisoService } from '../../../core/services/aviso.service';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AvisoDTO } from '../../../shared/models/aviso.models';

describe('TutoriaisComponent', () => {
  let component: TutoriaisComponent;
  let fixture: ComponentFixture<TutoriaisComponent>;
  let avisoServiceSpy: jasmine.SpyObj<AvisoService>;
  let assistenteServiceSpy: jasmine.SpyObj<AssistenteTutoriaService>;

  beforeEach(() => {
    avisoServiceSpy = jasmine.createSpyObj('AvisoService', ['listarAtivos']);
    assistenteServiceSpy = jasmine.createSpyObj('AssistenteTutoriaService', ['conversarCliente']);

    const avisosMock: AvisoDTO[] = [
      {
        id: 1,
        tipo: 'TUTORIAL',
        titulo: 'Como usar a Google Agenda',
        conteudo: 'Guia oficial para sincronizar calendários.',
        ativo: true
      },
      {
        id: 2,
        tipo: 'MANUTENCAO',
        titulo: 'Janela de manutenção',
        conteudo: 'Serviço temporariamente instável.',
        ativo: true
      }
    ];
    avisoServiceSpy.listarAtivos.and.returnValue(of(avisosMock));
    assistenteServiceSpy.conversarCliente.and.returnValue(of({
      resposta: 'Resposta mockada',
      modo: 'SUPORTE',
      dataHora: '2026-11-03T16:00:00Z',
      sugestoes: []
    }));

    TestBed.configureTestingModule({
      imports: [TutoriaisComponent],
      providers: [
        { provide: AvisoService, useValue: avisoServiceSpy },
        { provide: AssistenteTutoriaService, useValue: assistenteServiceSpy }
      ]
    });

    fixture = TestBed.createComponent(TutoriaisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('inicializa e carrega somente avisos do tipo TUTORIAL', () => {
    expect(component.carregandoAvisos()).toBeFalse();
    expect(component.tutoriaisSistema().length).toBe(1);
    expect(component.tutoriaisSistema()[0].titulo).toBe('Como usar a Google Agenda');
  });

  it('possui os 5 guias passo a passo estruturados', () => {
    expect(component.guias.length).toBe(5);
    expect(component.guias[0].id).toBe('whatsapp');
    expect(component.guias[4].id).toBe('simulador');
  });

  it('permite alternar expansao de cards', () => {
    expect(component.cardExpandido()).toBe('whatsapp');
    component.alternarCard('horarios');
    expect(component.cardExpandido()).toBe('horarios');
    component.alternarCard('horarios');
    expect(component.cardExpandido()).toBeNull();
  });

  it('aciona assistente ao pedir para tirar duvida', () => {
    spyOn(component.assistenteRef!, 'usarSugestao');
    component.tirarDuvidaNoAssistente('Como conectar o WhatsApp?');
    expect(component.assistenteRef?.usarSugestao).toHaveBeenCalledWith('Como conectar o WhatsApp?');
  });

  it('abre simulador virtual ao clicar no botao de teste', () => {
    spyOn(component.assistenteRef!, 'alternarModo');
    component.abrirSimuladorVirtual();
    expect(component.assistenteRef?.alternarModo).toHaveBeenCalledWith('TESTE_VIRTUAL');
  });
});
