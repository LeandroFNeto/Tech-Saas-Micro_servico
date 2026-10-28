import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { PrevisaoService } from '../../../core/services/previsao.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { PrevisaoDia, PrevisaoTempoResponse } from '../../../shared/models/previsao.models';
import { PainelComponent } from './painel.component';

function dia(data: string, sobrescrever: Partial<PrevisaoDia> = {}): PrevisaoDia {
  return {
    data,
    diaSemana: 'terça-feira',
    reservado: false,
    resumo: 'céu limpo',
    codigoTempo: 800,
    tempMax: 29,
    tempMin: 17,
    chanceChuva: 10,
    chuvaMm: 0,
    risco: 'BAIXO',
    estimativa: false,
    ...sobrescrever
  };
}

const PREVISAO: PrevisaoTempoResponse = {
  disponivel: true,
  reservasDisponiveis: true,
  atualizadoEm: '2026-10-27T14:05:00',
  cidade: 'Atibaia, São Paulo, BR',
  dias: [dia('2026-10-27'), dia('2026-10-28', { reservado: true, codigoTempo: 501, risco: 'ALTO' }), dia('2026-11-03', { estimativa: true })]
};

interface Cenario {
  empresa?: Record<string, unknown>;
  previsao?: Observable<PrevisaoTempoResponse>;
  atualizar?: Observable<unknown>;
}

describe('PainelComponent', () => {
  let atualizarPeloCliente: jasmine.Spy;
  let buscar: jasmine.Spy;

  function montar(cenario: Cenario = {}) {
    const empresa = cenario.empresa ?? { cidadePrevisao: 'Atibaia', previsaoTempoAtiva: true };
    atualizarPeloCliente = jasmine.createSpy('atualizarPeloCliente').and.returnValue(cenario.atualizar ?? of(empresa));
    buscar = jasmine.createSpy('buscar').and.returnValue(cenario.previsao ?? of(PREVISAO));
    TestBed.configureTestingModule({
      imports: [PainelComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { sessao: () => ({ sessaoWhatsapp: 'quadra', sub: 'quadra@gamb.com.br', papel: 'CLIENTE' }) }
        },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => NEVER, iniciar: () => NEVER } },
        { provide: EmpresaService, useValue: { buscarPorSessao: () => of(empresa), atualizarPeloCliente } },
        { provide: PrevisaoService, useValue: { buscar } }
      ]
    });
    const fixture = TestBed.createComponent(PainelComponent);
    fixture.detectChanges();
    return fixture;
  }

  function erroHttp(status: number, message?: string) {
    return throwError(() => new HttpErrorResponse({ status, error: message ? { message } : null }));
  }

  it('usa a sessao do WhatsApp do cliente logado', () => {
    expect(montar().componentInstance.sessao).toBe('quadra');
  });

  it('o botao da configuracao do chatbot leva a /cliente/chatbot', () => {
    const raiz: HTMLElement = montar().nativeElement;
    const link = raiz.querySelector('a[href="/cliente/chatbot"]');
    expect(link?.textContent).toContain('Configuração do chatbot');
  });

  it('mostra a conexao do WhatsApp', () => {
    const raiz: HTMLElement = montar().nativeElement;
    expect(raiz.querySelector('app-conexao-whatsapp')).not.toBeNull();
  });

  it('nao mostra mais o seletor so para visualizacao', () => {
    const raiz: HTMLElement = montar().nativeElement;
    expect(raiz.textContent).not.toContain('Só para visualização');
    expect(raiz.querySelector('select')).toBeNull();
  });

  it('mostra os cartoes da API, a atribuicao OpenWeather, a cidade resolvida e a hora', () => {
    const raiz: HTMLElement = montar().nativeElement;
    expect(buscar).toHaveBeenCalledWith('quadra');
    expect(raiz.querySelectorAll('ul li').length).toBe(3);
    expect(raiz.textContent).toContain('Dados meteorológicos: OpenWeather');
    expect(raiz.textContent).toContain('Atibaia, São Paulo, BR');
    expect(raiz.textContent).toContain('Atualizado às 14:05');
    expect(raiz.textContent).toContain('27/10');
  });

  it('marca reservado e estimativa so nos dias certos', () => {
    const raiz: HTMLElement = montar().nativeElement;
    const cartoes = Array.from(raiz.querySelectorAll('ul li'));
    expect(cartoes[0].textContent).not.toContain('Reservado');
    expect(cartoes[1].textContent).toContain('Reservado');
    expect(cartoes[0].textContent).not.toContain('estimativa');
    expect(cartoes[2].textContent).toContain('estimativa');
  });

  it('mostra atribuicao so quando ha cartoes', () => {
    const raiz: HTMLElement = montar({
      previsao: of({ ...PREVISAO, dias: [] })
    }).nativeElement;
    expect(raiz.querySelectorAll('ul li').length).toBe(0);
    expect(raiz.textContent).not.toContain('Dados meteorológicos: OpenWeather');
    expect(raiz.textContent).toContain('Nenhum dia de previsão para mostrar agora.');
  });

  it('mostra previsao indisponivel quando disponivel=false', () => {
    const raiz: HTMLElement = montar({
      previsao: of({ ...PREVISAO, disponivel: false, dias: [] })
    }).nativeElement;
    expect(raiz.textContent).toContain('Previsão indisponível no momento');
    expect(raiz.textContent).not.toContain('Dados meteorológicos: OpenWeather');
  });

  it('mostra previsao indisponivel quando a chamada falha', () => {
    const raiz: HTMLElement = montar({ previsao: erroHttp(500) }).nativeElement;
    expect(raiz.textContent).toContain('Previsão indisponível no momento');
  });

  it('mostra reservas indisponiveis e nenhum dia reservado', () => {
    const raiz: HTMLElement = montar({
      previsao: of({ ...PREVISAO, reservasDisponiveis: false, dias: PREVISAO.dias.map((d) => ({ ...d, reservado: false })) })
    }).nativeElement;
    expect(raiz.textContent).toContain('Reservas indisponíveis no momento');
    expect(raiz.querySelectorAll('ul li').length).toBe(3);
    expect(raiz.querySelector('ul li')?.textContent).not.toContain('Reservado');
  });

  it('sem cidade abre o formulario e nao chama a previsao', () => {
    const raiz: HTMLElement = montar({ empresa: { previsaoTempoAtiva: true } }).nativeElement;
    expect(buscar).not.toHaveBeenCalled();
    expect(raiz.textContent).toContain('Informe a cidade para ver a previsão.');
    expect(raiz.querySelector('#painel-cidade')).not.toBeNull();
  });

  it('com o interruptor desligado mostra o aviso e nao chama a previsao', () => {
    const raiz: HTMLElement = montar({ empresa: { cidadePrevisao: 'Atibaia', previsaoTempoAtiva: false } }).nativeElement;
    expect(buscar).not.toHaveBeenCalled();
    expect(raiz.textContent).toContain('A previsão do tempo está desligada');
  });

  it('salva cidade e interruptor pelo PUT do cliente e recarrega a previsao', () => {
    const fixture = montar();
    const comp = fixture.componentInstance;
    comp.abrirEdicao();
    comp.cidade.set('  Bragança  ');
    comp.previsaoAtiva.set(true);
    comp.salvarCidade();
    expect(atualizarPeloCliente).toHaveBeenCalledWith('quadra', { cidadePrevisao: 'Bragança', previsaoTempoAtiva: true });
    expect(buscar).toHaveBeenCalledTimes(2);
    expect(comp.sucesso()).toBeTrue();
    expect(comp.editando()).toBeFalse();
  });

  it('desabilita o botao enquanto salva', () => {
    const fixture = montar({ atualizar: NEVER });
    const comp = fixture.componentInstance;
    comp.abrirEdicao();
    fixture.detectChanges();
    comp.salvarCidade();
    fixture.detectChanges();
    const botao = (fixture.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(botao.disabled).toBeTrue();
    expect(botao.textContent).toContain('Salvando...');
  });

  const erros: [number, string | undefined, string][] = [
    [400, undefined, 'Cidade não encontrada. Confira o nome da cidade.'],
    [429, undefined, 'Muitas alterações de cidade hoje. Tente amanhã.'],
    [503, undefined, 'Não foi possível verificar a cidade agora. Tente novamente em alguns minutos.'],
    [400, 'Mensagem do back', 'Mensagem do back']
  ];

  erros.forEach(([status, message, esperado]) => {
    it(`trata o erro ${status}${message ? ' com a mensagem do back' : ''} mantendo a cidade digitada`, () => {
      const fixture = montar({ atualizar: erroHttp(status, message) });
      const comp = fixture.componentInstance;
      comp.abrirEdicao();
      comp.cidade.set('Atibaiaa');
      comp.salvarCidade();
      fixture.detectChanges();
      const raiz: HTMLElement = fixture.nativeElement;
      expect(raiz.querySelector('[role="alert"]')?.textContent).toContain(esperado);
      expect(comp.cidade()).toBe('Atibaiaa');
      expect((raiz.querySelector('#painel-cidade') as HTMLInputElement).value).toBe('Atibaiaa');
      expect(comp.salvando()).toBeFalse();
    });
  });
});
