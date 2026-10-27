import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { NEVER, of } from 'rxjs';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { EmpresaFormComponent } from './empresa-form.component';

describe('EmpresaFormComponent regressao visual', () => {
  const empresa = {
    nome: 'Quadra Teste',
    sessaoWhatsapp: 'quadra',
    ramoDeAtuacao: 'locacao',
    locacaoPorHora: true,
    googleCalendarId: 'agenda@group.calendar.google.com',
    linkFotoPrincipal: 'https://exemplo.com/a.jpg',
    urlsGaleria: ['https://exemplo.com/b.jpg'],
    linkGaleria: '',
    modulosAtivos: []
  };

  function montar(sessao: string) {
    TestBed.configureTestingModule({
      imports: [EmpresaFormComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ sessao }) } } },
        { provide: EmpresaService, useValue: { buscarPorSessao: jasmine.createSpy('buscarPorSessao').and.returnValue(of(empresa)) } },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => NEVER, iniciar: () => NEVER } }
      ]
    });
    const fixture = TestBed.createComponent(EmpresaFormComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  it('cadastro novo: todo campo tem fonte computada de pelo menos 16px', () => {
    const fixture = montar('nova');
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    expect(medirCampos(raiz).length).toBeGreaterThan(0);
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });

  it('edicao: Sessao WhatsApp e ID da agenda Google tem pelo menos 16px', () => {
    const fixture = montar('quadra');
    const raiz: HTMLElement = fixture.nativeElement;
    garantirTailwindCarregado(raiz);
    const campos = medirCampos(raiz).map((campo) => campo.descricao);
    expect(campos.some((descricao) => descricao.includes('sessaoWhatsapp'))).toBeTrue();
    expect(campos.some((descricao) => descricao.includes('googleCalendarId'))).toBeTrue();
    expect(camposAbaixoDoMinimo(raiz)).toEqual([]);
    raiz.remove();
  });
});
