import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AssistenteClienteComponent } from './assistente-cliente.component';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';
import { AssistenteChatResponseDTO } from '../../models/assistente.models';

describe('AssistenteClienteComponent', () => {
  let component: AssistenteClienteComponent;
  let fixture: ComponentFixture<AssistenteClienteComponent>;
  let assistenteServiceSpy: jasmine.SpyObj<AssistenteTutoriaService>;

  const respostaPadrao: AssistenteChatResponseDTO = {
    resposta: 'Passo a passo de teste retornado pelo backend.',
    modo: 'SUPORTE',
    dataHora: '2026-11-03T16:00:00Z',
    sugestoes: ['Nova sugestao']
  };

  beforeEach(() => {
    assistenteServiceSpy = jasmine.createSpyObj('AssistenteTutoriaService', ['conversarCliente', 'conversarPublico']);

    TestBed.configureTestingModule({
      imports: [AssistenteClienteComponent],
      providers: [{ provide: AssistenteTutoriaService, useValue: assistenteServiceSpy }]
    });

    fixture = TestBed.createComponent(AssistenteClienteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('inicia com mensagem de boas-vindas do Suporte', () => {
    expect(component.mensagens().length).toBe(1);
    expect(component.mensagens()[0].origem).toBe('ia');
    expect(component.mensagens()[0].texto).toContain('assistente de suporte');
  });

  it('usa saudacao, titulo e sugestoes iniciais e envia a origem quando informados', () => {
    const fixture2 = TestBed.createComponent(AssistenteClienteComponent);
    const comp = fixture2.componentInstance;
    comp.publico = true;
    comp.origem = 'LOGIN';
    comp.titulo = 'Fale com a Gamb';
    comp.saudacao = 'Ola visitante';
    comp.sugestoesIniciais = ['Quem é a Gamb?'];
    assistenteServiceSpy.conversarPublico.and.returnValue(of(respostaPadrao));
    fixture2.detectChanges();

    expect(comp.mensagens()[0].texto).toBe('Ola visitante');
    expect(comp.sugestoes()).toEqual(['Quem é a Gamb?']);
    expect((fixture2.nativeElement as HTMLElement).textContent).toContain('Fale com a Gamb');

    comp.mensagemInput = 'Quem é a Gamb?';
    comp.enviar();
    expect(assistenteServiceSpy.conversarPublico).toHaveBeenCalledWith({ mensagem: 'Quem é a Gamb?', origem: 'LOGIN' });
  });

  it('nao exibe Simulador nem alternancia de modo', () => {
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).not.toContain('Simulador');
    expect(texto).not.toContain('Sessão Virtual');
  });

  it('envia mensagem e adiciona resposta da IA na conversa', () => {
    assistenteServiceSpy.conversarCliente.and.returnValue(of(respostaPadrao));

    component.mensagemInput = 'Como conectar WhatsApp?';
    component.enviar();

    expect(assistenteServiceSpy.conversarCliente).toHaveBeenCalledWith({
      mensagem: 'Como conectar WhatsApp?',
      modo: 'SUPORTE'
    });
    expect(component.mensagens().length).toBe(3);
    expect(component.mensagens()[1].origem).toBe('usuario');
    expect(component.mensagens()[2].texto).toBe('Passo a passo de teste retornado pelo backend.');
    expect(component.sugestoes()).toEqual(['Nova sugestao']);
  });

  it('no modo publico chama o endpoint sem login', () => {
    assistenteServiceSpy.conversarPublico.and.returnValue(of(respostaPadrao));
    component.publico = true;

    component.mensagemInput = 'O que o app faz?';
    component.enviar();

    expect(assistenteServiceSpy.conversarPublico).toHaveBeenCalledWith({ mensagem: 'O que o app faz?' });
    expect(assistenteServiceSpy.conversarCliente).not.toHaveBeenCalled();
  });

  it('trata erro ao enviar mensagem exibindo mensagem de erro amigavel', () => {
    assistenteServiceSpy.conversarCliente.and.returnValue(throwError(() => new Error('Falha de rede')));

    component.mensagemInput = 'Teste de erro';
    component.enviar();

    expect(component.erro()).toContain('Não foi possível obter resposta do assistente');
    expect(component.enviando()).toBeFalse();
  });

  it('avisa quando o limite de mensagens por minuto e atingido', () => {
    assistenteServiceSpy.conversarPublico.and.returnValue(throwError(() => ({ status: 429 })));
    component.publico = true;

    component.mensagemInput = 'Outra duvida';
    component.enviar();

    expect(component.erro()).toContain('Muitas mensagens');
  });

  it('usarSugestao so preenche o campo e nao envia', () => {
    component.usarSugestao('Duvida rapida');

    expect(component.mensagemInput).toBe('Duvida rapida');
    expect(assistenteServiceSpy.conversarCliente).not.toHaveBeenCalled();
    expect(assistenteServiceSpy.conversarPublico).not.toHaveBeenCalled();
  });

  it('exibe Fechar apenas quando fechavel e emite o evento', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Fechar');

    component.fechavel = true;
    fixture.changeDetectorRef.detectChanges();
    let emitido = false;
    component.fechar.subscribe(() => (emitido = true));
    const botao = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Fechar'
    );
    botao?.click();

    expect(botao).toBeTruthy();
    expect(emitido).toBeTrue();
  });
});
