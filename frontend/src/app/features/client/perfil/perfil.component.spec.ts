import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { PerfilComponent } from './perfil.component';

describe('PerfilComponent regressao visual', () => {
  const empresa = {
    nome: 'Quadra Teste',
    sessaoWhatsapp: 'quadra',
    urlsGaleria: ['https://exemplo.com/b.jpg'],
    modulosMenu: [],
    modulosAtivos: []
  };

  function montar() {
    TestBed.configureTestingModule({
      imports: [PerfilComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { sessao: () => ({ sessaoWhatsapp: 'quadra', sub: 'quadra@gamb.com.br', papel: 'CLIENTE' }) }
        },
        { provide: EmpresaService, useValue: { buscarPorSessao: () => of(empresa) } },
        { provide: AvisoService, useValue: { listarAtivos: () => of([]) } }
      ]
    });
    const fixture = TestBed.createComponent(PerfilComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  for (const aba of ['dados', 'robo', 'configuracao'] as const) {
    it(`aba ${aba}: todo campo tem fonte computada de pelo menos 16px`, () => {
      const fixture = montar();
      fixture.componentInstance.aba.set(aba);
      fixture.detectChanges();
      const raiz: HTMLElement = fixture.nativeElement;
      garantirTailwindCarregado(raiz);
      expect(medirCampos(raiz).length).toBeGreaterThan(0);
      expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
      raiz.remove();
    });
  }

  it('mostra o link Voltar ao painel para /cliente e nao mostra a conexao do WhatsApp', () => {
    const fixture = montar();
    const raiz: HTMLElement = fixture.nativeElement;
    const link = raiz.querySelector('a[href="/cliente"]');
    expect(link?.textContent).toContain('Voltar ao painel');
    expect(raiz.querySelector('app-conexao-whatsapp')).toBeNull();
    raiz.remove();
  });
});
