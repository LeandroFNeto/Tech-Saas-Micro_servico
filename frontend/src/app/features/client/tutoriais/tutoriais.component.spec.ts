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
    localStorage.removeItem('gamb.tutoriais.progresso');
    avisoServiceSpy = jasmine.createSpyObj('AvisoService', ['listarAtivos']);
    assistenteServiceSpy = jasmine.createSpyObj('AssistenteTutoriaService', ['conversarCliente']);

    const avisosMock: AvisoDTO[] = [
      { id: 1, tipo: 'TUTORIAL', titulo: 'Como usar a Google Agenda', conteudo: 'Guia oficial para sincronizar calendários.', ativo: true },
      { id: 2, tipo: 'MANUTENCAO', titulo: 'Janela de manutenção', conteudo: 'Serviço temporariamente instável.', ativo: true }
    ];
    avisoServiceSpy.listarAtivos.and.returnValue(of(avisosMock));
    assistenteServiceSpy.conversarCliente.and.returnValue(
      of({ resposta: 'Resposta mockada', modo: 'SUPORTE', dataHora: '2026-11-03T16:00:00Z', sugestoes: [] })
    );

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

  afterEach(() => localStorage.removeItem('gamb.tutoriais.progresso'));

  it('inicializa e carrega somente avisos do tipo TUTORIAL', () => {
    expect(component.carregandoAvisos()).toBeFalse();
    expect(component.tutoriaisSistema().length).toBe(1);
    expect(component.tutoriaisSistema()[0].titulo).toBe('Como usar a Google Agenda');
  });

  it('exibe o chat antes dos cartoes de guias', () => {
    const html = fixture.nativeElement as HTMLElement;
    const chat = html.querySelector('#chat');
    const cartoes = html.querySelector('#titulo-guias-passo');

    expect(chat).not.toBeNull();
    expect(chat!.compareDocumentPosition(cartoes!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('lista os guias sem o Simulador', () => {
    expect(component.guias.map((g) => g.id)).toEqual(['whatsapp', 'horarios', 'agenda', 'regras-fotos']);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Simulador');
  });

  it('comeca com o selo Novo em todos os guias', () => {
    expect(component.guias.every((g) => component.selo(g.id) === 'Novo')).toBeTrue();
  });

  it('abre o tutorial, avanca os passos e marca Em andamento', () => {
    component.abrir('whatsapp');
    expect(component.passoAtual).toBe(0);

    component.proximo();
    expect(component.passoAtual).toBe(1);
    expect(component.percentualProgresso).toBe(40);

    component.voltar();
    expect(component.selo('whatsapp')).toBe('Em andamento');
  });

  it('conclui o tutorial no ultimo passo e guarda no navegador', () => {
    component.abrir('whatsapp');
    for (let i = 0; i < 5; i++) {
      component.proximo();
    }

    expect(component.selo('whatsapp')).toBe('Concluído');
    expect(component.guiaAberto()).toBeNull();
    expect(localStorage.getItem('gamb.tutoriais.progresso')).toContain('"concluido":true');
  });

  it('Anterior volta um passo sem passar do primeiro', () => {
    component.abrir('horarios');
    component.anterior();
    expect(component.passoAtual).toBe(0);

    component.proximo();
    component.anterior();
    expect(component.passoAtual).toBe(0);
  });

  it('Perguntar ao assistente sobre este passo so preenche o campo e nao envia', () => {
    component.abrir('whatsapp');
    component.proximo();
    fixture.detectChanges();

    component.perguntarSobrePasso();

    expect(component.assistenteRef!.mensagemInput).toContain('passo 2');
    expect(assistenteServiceSpy.conversarCliente).not.toHaveBeenCalled();
  });
});
