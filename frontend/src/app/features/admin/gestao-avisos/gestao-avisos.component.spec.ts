import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { AvisoDTO } from '../../../shared/models/aviso.models';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { GestaoAvisosComponent } from './gestao-avisos.component';

describe('GestaoAvisosComponent', () => {
  const avisoExistente: AvisoDTO = {
    id: 5,
    tipo: 'MANUTENCAO',
    titulo: 'Manutenção programada',
    conteudo: 'Pausa às 23h',
    link: 'https://gamb.site/status',
    ativo: true
  };

  function montar(superAdmin = true, avisosIniciais: AvisoDTO[] = [avisoExistente]) {
    const avisosSpy = jasmine.createSpyObj<AvisoService>('AvisoService', [
      'listarAdmin',
      'criar',
      'atualizar',
      'excluir'
    ]);
    avisosSpy.listarAdmin.and.returnValue(of(avisosIniciais));
    avisosSpy.criar.and.returnValue(of({ ...avisoExistente, id: 9 }));
    avisosSpy.atualizar.and.returnValue(of(avisoExistente));
    avisosSpy.excluir.and.returnValue(of(undefined));

    TestBed.configureTestingModule({
      imports: [GestaoAvisosComponent],
      providers: [
        { provide: AvisoService, useValue: avisosSpy },
        {
          provide: AuthService,
          useValue: {
            isAdmin: () => true,
            isSuperAdmin: () => superAdmin,
            isLeitura: () => !superAdmin,
            possuiPapel: () => true
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(GestaoAvisosComponent);
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, avisosSpy };
  }

  it('todo campo de formulario tem fonte computada de pelo menos 16px', () => {
    const { fixture } = montar(true, []);
    const raiz: HTMLElement = fixture.nativeElement;
    document.body.appendChild(raiz);
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });

  it('lista avisos existentes e oculta formulario e botoes de acao quando nao e super admin', () => {
    const { fixture } = montar(false, [avisoExistente]);
    const raiz: HTMLElement = fixture.nativeElement;

    expect(raiz.textContent).toContain('Manutenção programada');
    expect(raiz.querySelector('form')).toBeNull();
    expect(raiz.querySelector('button[aria-label*="Editar aviso"]')).toBeNull();
    expect(raiz.querySelector('button[aria-label*="Excluir aviso"]')).toBeNull();
  });

  it('cria novo aviso via POST e recarrega a lista', () => {
    const { comp, avisosSpy } = montar(true, []);

    comp.salvar();
    expect(avisosSpy.criar).not.toHaveBeenCalled();

    comp.form.setValue({
      titulo: 'Novo tutorial',
      tipo: 'TUTORIAL',
      conteudo: 'Passo a passo',
      link: '   ',
      ativo: true
    });
    comp.salvar();

    expect(avisosSpy.criar).toHaveBeenCalledWith({
      titulo: 'Novo tutorial',
      tipo: 'TUTORIAL',
      conteudo: 'Passo a passo',
      link: null,
      ativo: true
    });
    expect(comp.sucesso()).toBe('Aviso criado.');
  });

  it('edita aviso via PUT e permite cancelarEdicao', () => {
    const { comp, avisosSpy } = montar(true);

    comp.editar(avisoExistente);
    expect(comp.editandoId()).toBe(5);
    expect(comp.form.controls.titulo.value).toBe('Manutenção programada');

    comp.cancelarEdicao();
    expect(comp.editandoId()).toBeNull();
    expect(comp.form.controls.titulo.value).toBe('');

    comp.editar(avisoExistente);
    comp.form.patchValue({ titulo: 'Manutenção adiada' });
    comp.salvar();

    expect(avisosSpy.atualizar).toHaveBeenCalledWith(5, jasmine.objectContaining({ titulo: 'Manutenção adiada' }));
    expect(comp.sucesso()).toBe('Aviso atualizado.');
    expect(comp.editandoId()).toBeNull();
  });

  it('exclui aviso somente quando confirm retorna true e limpa edicao ativa', () => {
    const { comp, avisosSpy } = montar(true);
    const confirmSpy = spyOn(window, 'confirm');

    comp.excluir({ ...avisoExistente, id: undefined });
    expect(confirmSpy).not.toHaveBeenCalled();

    confirmSpy.and.returnValue(false);
    comp.excluir(avisoExistente);
    expect(avisosSpy.excluir).not.toHaveBeenCalled();

    comp.editar(avisoExistente);
    confirmSpy.and.returnValue(true);
    comp.excluir(avisoExistente);

    expect(avisosSpy.excluir).toHaveBeenCalledWith(5);
    expect(comp.editandoId()).toBeNull();
    expect(comp.sucesso()).toBe('Aviso excluído.');
  });

  it('exibe erros HTTP ao carregar, salvar ou excluir', () => {
    const { comp, avisosSpy } = montar(true);

    avisosSpy.listarAdmin.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Falha ao listar' } }))
    );
    comp.carregar();
    expect(comp.erro()).toBe('Falha ao listar');

    avisosSpy.criar.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'Título inválido' } }))
    );
    comp.form.setValue({ titulo: 'A', tipo: 'COMUNICADO', conteudo: 'B', link: '', ativo: true });
    comp.salvar();
    expect(comp.erro()).toBe('Título inválido');
    expect(comp.salvando()).toBeFalse();

    spyOn(window, 'confirm').and.returnValue(true);
    avisosSpy.excluir.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { message: 'Sem permissão' } }))
    );
    comp.excluir(avisoExistente);
    expect(comp.erro()).toBe('Sem permissão');
  });
});
