import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AssistenteClienteComponent } from './assistente-cliente.component';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AssistenteChatResponseDTO } from '../../models/assistente.models';

describe('AssistenteClienteComponent', () => {
  let component: AssistenteClienteComponent;
  let fixture: ComponentFixture<AssistenteClienteComponent>;
  let assistenteServiceSpy: jasmine.SpyObj<AssistenteTutoriaService>;

  beforeEach(() => {
    assistenteServiceSpy = jasmine.createSpyObj('AssistenteTutoriaService', ['conversarCliente']);

    TestBed.configureTestingModule({
      imports: [AssistenteClienteComponent],
      providers: [
        { provide: AssistenteTutoriaService, useValue: assistenteServiceSpy }
      ]
    });

    fixture = TestBed.createComponent(AssistenteClienteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('inicia no modo SUPORTE com mensagem de boas-vindas', () => {
    expect(component.modo()).toBe('SUPORTE');
    expect(component.mensagens().length).toBe(1);
    expect(component.mensagens()[0].origem).toBe('ia');
    expect(component.mensagens()[0].texto).toContain('assistente de suporte');
  });

  it('alterna para modo TESTE_VIRTUAL e ajusta mensagem introdutoria e sugestoes', () => {
    component.alternarModo('TESTE_VIRTUAL');
    expect(component.modo()).toBe('TESTE_VIRTUAL');
    expect(component.mensagens()[0].texto).toContain('Sessão Virtual de Testes');
    expect(component.sugestoes()[0]).toContain('preços e regras');
  });

  it('envia mensagem e adiciona resposta da IA na conversa', () => {
    const mockResp: AssistenteChatResponseDTO = {
      resposta: 'Passo a passo de teste retornado pelo backend.',
      modo: 'SUPORTE',
      dataHora: '2026-11-03T16:00:00Z',
      sugestoes: ['Nova sugestao']
    };
    assistenteServiceSpy.conversarCliente.and.returnValue(of(mockResp));

    component.mensagemInput = 'Como conectar WhatsApp?';
    component.enviar();

    expect(assistenteServiceSpy.conversarCliente).toHaveBeenCalledWith({
      mensagem: 'Como conectar WhatsApp?',
      modo: 'SUPORTE'
    });
    expect(component.mensagens().length).toBe(3);
    expect(component.mensagens()[1].origem).toBe('usuario');
    expect(component.mensagens()[1].texto).toBe('Como conectar WhatsApp?');
    expect(component.mensagens()[2].origem).toBe('ia');
    expect(component.mensagens()[2].texto).toBe('Passo a passo de teste retornado pelo backend.');
    expect(component.sugestoes()).toEqual(['Nova sugestao']);
  });

  it('trata erro ao enviar mensagem exibindo mensagem de erro amigavel', () => {
    assistenteServiceSpy.conversarCliente.and.returnValue(throwError(() => new Error('Falha de rede')));

    component.mensagemInput = 'Teste de erro';
    component.enviar();

    expect(component.erro()).toContain('Não foi possível obter resposta do assistente');
    expect(component.enviando()).toBeFalse();
  });

  it('usarSugestao preenche o input e envia automaticamente', () => {
    const mockResp: AssistenteChatResponseDTO = {
      resposta: 'Resposta automatica.',
      modo: 'SUPORTE',
      dataHora: '2026-11-03T16:00:00Z',
      sugestoes: []
    };
    assistenteServiceSpy.conversarCliente.and.returnValue(of(mockResp));

    component.usarSugestao('Duvida rapida');
    expect(assistenteServiceSpy.conversarCliente).toHaveBeenCalledWith({
      mensagem: 'Duvida rapida',
      modo: 'SUPORTE'
    });
  });
});
