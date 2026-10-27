import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
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
        { provide: AvisoService, useValue: { listarAtivos: () => of([]) } },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => NEVER, iniciar: () => NEVER } }
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
});
