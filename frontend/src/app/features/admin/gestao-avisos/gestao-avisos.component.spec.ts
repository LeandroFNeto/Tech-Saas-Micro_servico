import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AvisoService } from '../../../core/services/aviso.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { GestaoAvisosComponent } from './gestao-avisos.component';

describe('GestaoAvisosComponent regressao visual', () => {
  it('todo campo de formulario tem fonte computada de pelo menos 16px', () => {
    TestBed.configureTestingModule({
      imports: [GestaoAvisosComponent],
      providers: [{ provide: AvisoService, useValue: { listarAdmin: jasmine.createSpy('listarAdmin').and.returnValue(of([])) } }]
    });
    const fixture = TestBed.createComponent(GestaoAvisosComponent);
    fixture.detectChanges();
    const raiz: HTMLElement = fixture.nativeElement;
    document.body.appendChild(raiz);
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });
});
