import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NEVER } from 'rxjs';
import { ChatFlutuanteComponent } from './chat-flutuante.component';
import { AssistenteTutoriaService } from '../../../core/services/assistente-tutoria.service';

describe('ChatFlutuanteComponent', () => {
  let component: ChatFlutuanteComponent;
  let fixture: ComponentFixture<ChatFlutuanteComponent>;
  let elemento: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ChatFlutuanteComponent],
      providers: [
        { provide: AssistenteTutoriaService, useValue: { conversarCliente: () => NEVER, conversarPublico: () => NEVER } }
      ]
    });

    fixture = TestBed.createComponent(ChatFlutuanteComponent);
    component = fixture.componentInstance;
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('nasce fechado, so com o botao', () => {
    expect(component.aberto()).toBeFalse();
    expect(elemento.querySelector('[role="dialog"]')).toBeNull();
    expect(elemento.querySelector('button')).not.toBeNull();
  });

  it('abre o painel ao acionar o botao e fecha por Fechar', () => {
    (elemento.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(elemento.querySelector('[role="dialog"]')).not.toBeNull();

    const fechar = Array.from(elemento.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Fechar');
    fechar?.click();
    fixture.detectChanges();

    expect(component.aberto()).toBeFalse();
    expect(elemento.querySelector('[role="dialog"]')).toBeNull();
  });

  it('mostra a dica apenas com o chat fechado', () => {
    fixture.componentRef.setInput('dica', 'Para mais informações, fale com o chat agora');
    fixture.detectChanges();
    expect(elemento.textContent).toContain('fale com o chat agora');

    component.alternar();
    fixture.detectChanges();
    expect(elemento.textContent).not.toContain('fale com o chat agora');
  });

  it('sobe 76 px sobre as abas quando acimaDasAbas', () => {
    fixture.componentRef.setInput('acimaDasAbas', true);
    fixture.detectChanges();

    const botao = elemento.querySelector('button') as HTMLButtonElement;
    expect(botao.className).toContain('bottom-[76px]');
  });
});
