import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ContatosComponent } from './contatos.component';
import { ContatoService } from '../../../core/services/contato.service';
import { ContatoResumoDTO } from '../../../shared/models/contato.models';

describe('ContatosComponent', () => {
  let component: ContatosComponent;
  let fixture: ComponentFixture<ContatosComponent>;
  let contatoServiceSpy: jasmine.SpyObj<ContatoService>;

  const contatosMock: ContatoResumoDTO[] = [
    {
      id: 1,
      nome: 'João Santos',
      telefoneMascarado: '(11) *****-1234',
      primeiraConversaEm: '2026-11-01T10:00:00',
      ultimaConversaEm: '2026-11-07T12:00:00',
      naoContatar: false,
      origem: 'CONVERSA'
    }
  ];

  beforeEach(() => {
    contatoServiceSpy = jasmine.createSpyObj('ContatoService', ['listar', 'revelarNumero', 'excluir']);
    contatoServiceSpy.listar.and.returnValue(of(contatosMock));
    contatoServiceSpy.revelarNumero.and.returnValue(of({ numero: '5511987651234' }));
    contatoServiceSpy.excluir.and.returnValue(of(void 0));

    TestBed.configureTestingModule({
      imports: [ContatosComponent],
      providers: [
        { provide: ContatoService, useValue: contatoServiceSpy }
      ]
    });

    fixture = TestBed.createComponent(ContatosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega e renderiza contatos com nome e mascara por padrao', () => {
    expect(component.carregando()).toBeFalse();
    expect(component.contatos().length).toBe(1);
    expect(component.contatos()[0].nome).toBe('João Santos');
    expect(component.contatos()[0].telefoneMascarado).toBe('(11) *****-1234');
  });

  it('revela numero completo ao chamar revelarNumero', () => {
    component.revelarNumero(contatosMock[0]);

    expect(contatoServiceSpy.revelarNumero).toHaveBeenCalledWith(1);
    expect(component.numerosRevelados()[1]).toBe('5511987651234');
    expect(component.erro()).toBeNull();
  });

  it('exibe mensagem de limite anti-vazamento quando API retorna 429', () => {
    contatoServiceSpy.revelarNumero.and.returnValue(throwError(() => ({ status: 429 })));

    component.revelarNumero(contatosMock[0]);

    expect(component.erro()).toContain('Limite de 10 contatos por hora atingido');
  });

  it('exclui contato ao confirmar e recarrega lista', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.excluir(contatosMock[0]);

    expect(contatoServiceSpy.excluir).toHaveBeenCalledWith(1);
    expect(contatoServiceSpy.listar).toHaveBeenCalledTimes(2);
  });

  it('nao exclui contato se usuario cancelar confirmacao', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.excluir(contatosMock[0]);

    expect(contatoServiceSpy.excluir).not.toHaveBeenCalled();
  });
});
