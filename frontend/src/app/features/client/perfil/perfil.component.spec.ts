import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { FuncionamentoService } from '../../../core/services/funcionamento.service';
import { camposAbaixoDoMinimo, garantirTailwindCarregado, medirCampos } from '../../../testing/campos-fonte.testing';
import { EmpresaView } from '../../../shared/models/empresa.models';
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
        {
          provide: FuncionamentoService,
          useValue: {
            ler: () =>
              of({
                duracaoAtendimentoMinutos: 60,
                intervaloEntreAtendimentosMinutos: 0,
                uf: null,
                horarios: [],
                diasEspeciais: []
              })
          }
        }
      ]
    });
    const fixture = TestBed.createComponent(PerfilComponent);
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
    return fixture;
  }

  for (const aba of ['dados', 'robo', 'funcionamento', 'configuracao'] as const) {
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

const FOTO = 'https://res.cloudinary.com/demo/hero.jpg';
const GALERIA = ['https://res.cloudinary.com/demo/a.jpg', 'https://res.cloudinary.com/demo/b.jpg'];

function empresaCom(fotos: boolean): EmpresaView {
  return {
    nome: 'Casa de Festas',
    sessaoWhatsapp: 'casa',
    modulosAtivos: [],
    modulosMenu: [],
    linkFotoPrincipal: fotos ? FOTO : undefined,
    urlsGaleria: fotos ? GALERIA : []
  };
}

function montarComFotos(empresa: EmpresaView): ComponentFixture<PerfilComponent> {
  TestBed.configureTestingModule({
    imports: [PerfilComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { sessao: () => ({ sessaoWhatsapp: 'casa', sub: 'cliente@casa.com' }) } },
      { provide: AvisoService, useValue: { listarAtivos: () => of([]) } },
      { provide: EmpresaService, useValue: { buscarPorSessao: () => of(empresa) } }
    ]
  });
  const fixture = TestBed.createComponent(PerfilComponent);
  fixture.detectChanges();
  return fixture;
}

function botao(fixture: ComponentFixture<PerfilComponent>, texto: string): HTMLButtonElement {
  const botoes: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
  const achado = botoes.find((b) => b.textContent?.trim() === texto);
  expect(achado).withContext(texto).toBeTruthy();
  return achado as HTMLButtonElement;
}

describe('PerfilComponent, reserva automatica so leitura', () => {
  function montarComEmpresaLigada() {
    const atualizarPeloCliente = jasmine.createSpy('atualizarPeloCliente').and.returnValue(of({}));
    TestBed.configureTestingModule({
      imports: [PerfilComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { sessao: () => ({ sessaoWhatsapp: 'casa', sub: 'cliente@casa.com' }) } },
        { provide: AvisoService, useValue: { listarAtivos: () => of([]) } },
        {
          provide: EmpresaService,
          useValue: {
            buscarPorSessao: () => of({ ...empresaCom(false), permiteReservaAutomatica: true }),
            atualizarPeloCliente
          }
        }
      ]
    });
    const fixture = TestBed.createComponent(PerfilComponent);
    fixture.detectChanges();
    return { fixture, atualizarPeloCliente };
  }

  it('salvar nao envia permiteReservaAutomatica', () => {
    const { fixture, atualizarPeloCliente } = montarComEmpresaLigada();

    fixture.componentInstance.salvar();

    expect(atualizarPeloCliente).toHaveBeenCalledTimes(1);
    const [sessao, dto] = atualizarPeloCliente.calls.mostRecent().args;
    expect(sessao).toBe('casa');
    expect(Object.keys(dto)).not.toContain('permiteReservaAutomatica');
    expect(dto.nome).toBe('Casa de Festas');
  });

  it('mostra a opcao desabilitada, marcada e com o aviso do administrador', () => {
    const { fixture } = montarComEmpresaLigada();
    fixture.componentInstance.aba.set('robo');
    fixture.detectChanges();
    const raiz: HTMLElement = fixture.nativeElement;

    const interruptor = raiz.querySelector<HTMLInputElement>('input[aria-labelledby="reserva-automatica-rotulo"]');
    expect(interruptor).not.toBeNull();
    expect(interruptor?.disabled).toBeTrue();
    expect(interruptor?.checked).toBeTrue();
    expect(raiz.querySelector('#reserva-automatica-aviso')?.textContent).toContain(
      'A reserva automática é liberada pelo administrador.'
    );
  });
});

describe('PerfilComponent, fotos pelo Cloudinary', () => {
  it('abre a foto principal e cada foto da galeria em nova aba', () => {
    const fixture = montarComFotos(empresaCom(true));
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a[target="_blank"]'));
    expect(links.map((a) => a.getAttribute('href'))).toEqual([FOTO, ...GALERIA]);
    expect(links.every((a) => a.getAttribute('rel')?.includes('noopener'))).toBeTrue();
  });

  it('não mostra link quando ainda não há foto', () => {
    const fixture = montarComFotos(empresaCom(false));
    expect(fixture.nativeElement.querySelectorAll('a[target="_blank"]').length).toBe(0);
  });

  it('os botões de envio abrem o widget da foto principal e da galeria', () => {
    const fixture = montarComFotos(empresaCom(false));
    const abrir = spyOn(fixture.componentInstance, 'abrirWidgetCloudinary');
    botao(fixture, 'Enviar foto principal').click();
    botao(fixture, 'Enviar fotos').click();
    expect(abrir.calls.allArgs()).toEqual([['principal'], ['galeria']]);
  });

  it('avisa quando o script do Cloudinary não carregou', () => {
    const fixture = montarComFotos(empresaCom(false));
    botao(fixture, 'Enviar foto principal').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('O widget do Cloudinary não carregou');
  });
});
