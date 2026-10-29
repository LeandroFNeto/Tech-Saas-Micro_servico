import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import contentData from '../../../../../../especificacao/front/cliente-perfil.content.json';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { FuncionamentoDTO } from '../../../shared/models/funcionamento.models';
import { FuncionamentoComponent } from './funcionamento.component';

describe('FuncionamentoComponent', () => {
  const textos = contentData.clientePerfil.funcionamento;
  const base = `${environment.apiUrl}/empresas/quadra`;
  let fixture: ComponentFixture<FuncionamentoComponent>;
  let componente: FuncionamentoComponent;
  let raiz: HTMLElement;
  let http: HttpTestingController;

  const completo: FuncionamentoDTO = {
    duracaoAtendimentoMinutos: 90,
    intervaloEntreAtendimentosMinutos: 15,
    uf: 'SP',
    horarios: [
      { perfil: 'SEGUNDA', fechado: false, abre: '08:00', fecha: '22:00', duracaoMinutos: null },
      { perfil: 'TERCA', fechado: false, abre: '08:00', fecha: '22:00', duracaoMinutos: null },
      { perfil: 'QUARTA', fechado: false, abre: '08:00', fecha: '22:00', duracaoMinutos: null },
      { perfil: 'QUINTA', fechado: false, abre: '08:00', fecha: '22:00', duracaoMinutos: null },
      { perfil: 'SEXTA', fechado: false, abre: '08:00', fecha: '23:00', duracaoMinutos: null },
      { perfil: 'SABADO', fechado: false, abre: '10:00', fecha: '22:00', duracaoMinutos: 120 },
      { perfil: 'DOMINGO', fechado: true, abre: null, fecha: null, duracaoMinutos: null },
      { perfil: 'FERIADO', fechado: false, abre: '10:00', fecha: '18:00', duracaoMinutos: null }
    ],
    diasEspeciais: [
      { id: 1, data: '2026-12-24', fechado: false, abre: '10:00', fecha: '16:00', motivo: 'Vespera' },
      { id: 2, data: '2026-12-08', fechado: true, abre: null, fecha: null, motivo: 'Feriado municipal' }
    ]
  };

  const vazioDto: FuncionamentoDTO = {
    duracaoAtendimentoMinutos: 60,
    intervaloEntreAtendimentosMinutos: 0,
    uf: null,
    horarios: [],
    diasEspeciais: []
  };

  function digitar(seletor: string, valor: string): void {
    const campo = raiz.querySelector<HTMLInputElement>(seletor)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function botaoComTexto(texto: string): HTMLButtonElement | undefined {
    return Array.from(raiz.querySelectorAll('button')).find((botao) => botao.textContent?.trim() === texto);
  }

  function somarDias(iso: string, dias: number): string {
    const [ano, mes, dia] = iso.split('-').map(Number);
    const data = new Date(ano, mes - 1, dia + dias);
    return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
  }

  function responderLeitura(dto: FuncionamentoDTO): void {
    http.expectOne(`${base}/funcionamento`).flush(dto);
    fixture.detectChanges();
  }

  function abrirPersonalizar(): void {
    if (!raiz.querySelector('#func-personalizar')) {
      botaoComTexto(textos.personalizar.abrir)!.click();
      fixture.detectChanges();
    }
  }

  function montar(sessao: string | null): void {
    TestBed.configureTestingModule({
      imports: [FuncionamentoComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sessao: () => (sessao ? { sessaoWhatsapp: sessao } : null) } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FuncionamentoComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
    raiz = fixture.nativeElement;
  }

  afterEach(() => http.verify());

  describe('com a sessao da empresa', () => {
    beforeEach(() => montar('quadra'));

    it('mostra carregando e depois o funcionamento lido da API', () => {
      expect(raiz.querySelector('[aria-busy="true"]')).not.toBeNull();
      expect(raiz.querySelector('form')).toBeNull();
      responderLeitura(completo);
      expect(raiz.querySelector('[aria-busy="true"]')).toBeNull();
      expect(raiz.querySelector<HTMLInputElement>('#func-abre')?.value).toBe('08:00');
      expect(raiz.querySelector<HTMLInputElement>('#func-fecha')?.value).toBe('22:00');
      expect(raiz.querySelector<HTMLSelectElement>('#func-duracao')?.value).toBe('90');
      expect(raiz.textContent).not.toContain(textos.vazio);
      expect(raiz.querySelector('#func-personalizar')).not.toBeNull();
      expect(raiz.querySelector<HTMLInputElement>('#func-abre-DOMINGO')?.disabled).toBeTrue();
      expect(raiz.querySelector<HTMLSelectElement>('#func-duracao-SABADO')?.value).toBe('120');
      expect(raiz.querySelector<HTMLInputElement>('#func-intervalo')?.value).toBe('15');
      expect(raiz.querySelector<HTMLSelectElement>('#func-uf')?.value).toBe('SP');
      const itens = Array.from(raiz.querySelectorAll('#func-personalizar ul li'));
      expect(itens.length).toBe(2);
      expect(itens[0].textContent).toContain('Feriado municipal');
    });

    it('sem horario salvo mostra o texto de vazio e o padrao 08:00 a 22:00 com 60 minutos', () => {
      responderLeitura(vazioDto);
      expect(raiz.textContent).toContain(textos.vazio);
      expect(raiz.querySelector<HTMLInputElement>('#func-abre')?.value).toBe('08:00');
      expect(raiz.querySelector<HTMLInputElement>('#func-fecha')?.value).toBe('22:00');
      expect(raiz.querySelector<HTMLSelectElement>('#func-duracao')?.value).toBe('60');
      expect(raiz.querySelector('#func-personalizar')).toBeNull();
    });

    it('erro mostra a mensagem e Tentar de novo refaz a leitura', () => {
      http.expectOne(`${base}/funcionamento`).flush({}, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(raiz.textContent).toContain(textos.erro.titulo);
      expect(raiz.textContent).toContain(textos.erro.texto);
      expect(raiz.querySelector('form')).toBeNull();
      botaoComTexto(textos.erro.tentar)!.click();
      fixture.detectChanges();
      expect(raiz.querySelector('[aria-busy="true"]')).not.toBeNull();
      responderLeitura(completo);
      expect(raiz.querySelector('form')).not.toBeNull();
    });

    it('desabilita Salvar quando fechar nao e depois de abrir', () => {
      responderLeitura(completo);
      digitar('#func-fecha', '07:00');
      expect(botaoComTexto(textos.salvar)?.disabled).toBeTrue();
      expect(raiz.textContent).toContain(textos.simples.erroHorario);
      expect(raiz.querySelector('#func-fecha')?.getAttribute('aria-invalid')).toBe('true');
      digitar('#func-fecha', '23:00');
      expect(botaoComTexto(textos.salvar)?.disabled).toBeFalse();
    });

    it('intervalo fora de 0 a 120 trava o Salvar', () => {
      responderLeitura(completo);
      digitar('#func-intervalo', '150');
      expect(botaoComTexto(textos.salvar)?.disabled).toBeTrue();
      digitar('#func-intervalo', '30');
      expect(botaoComTexto(textos.salvar)?.disabled).toBeFalse();
    });

    it('salvar no modo simples faz PUT com o mesmo horario nos 7 dias e no feriado', () => {
      responderLeitura(vazioDto);
      digitar('#func-abre', '09:00');
      digitar('#func-fecha', '21:30');
      botaoComTexto(textos.salvar)!.click();
      fixture.detectChanges();
      expect(botaoComTexto(textos.salvar)?.disabled).toBeTrue();

      const req = http.expectOne(`${base}/funcionamento`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.duracaoAtendimentoMinutos).toBe(60);
      expect(req.request.body.intervaloEntreAtendimentosMinutos).toBe(0);
      expect(req.request.body.uf).toBe('');
      expect(req.request.body.horarios.length).toBe(8);
      for (const horario of req.request.body.horarios) {
        expect(horario.fechado).toBeFalse();
        expect(horario.abre).toBe('09:00');
        expect(horario.fecha).toBe('21:30');
        expect(horario.duracaoMinutos).toBeNull();
      }
      expect(req.request.body.horarios.map((h: { perfil: string }) => h.perfil)).toEqual([
        'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO', 'DOMINGO', 'FERIADO'
      ]);
      req.flush({ ...vazioDto, horarios: req.request.body.horarios });
      fixture.detectChanges();
      expect(raiz.textContent).toContain(textos.sucesso);
      expect(botaoComTexto(textos.salvar)?.disabled).toBeFalse();
    });

    it('salvar no Personalizar envia fechado por dia e Usar a geral como null', () => {
      responderLeitura(completo);
      abrirPersonalizar();
      botaoComTexto(textos.salvar)!.click();
      const req: TestRequest = http.expectOne(`${base}/funcionamento`);
      expect(req.request.body.duracaoAtendimentoMinutos).toBe(90);
      expect(req.request.body.intervaloEntreAtendimentosMinutos).toBe(15);
      expect(req.request.body.uf).toBe('SP');
      const porPerfil = (perfil: string) =>
        req.request.body.horarios.find((h: { perfil: string }) => h.perfil === perfil);
      expect(porPerfil('DOMINGO')).toEqual({ perfil: 'DOMINGO', fechado: true, duracaoMinutos: null });
      expect(porPerfil('SABADO')).toEqual({
        perfil: 'SABADO', fechado: false, abre: '10:00', fecha: '22:00', duracaoMinutos: 120
      });
      expect(porPerfil('SEGUNDA').duracaoMinutos).toBeNull();
      req.flush(completo);
    });

    it('erro 400 ao salvar mostra a mensagem recebida e libera o botao', () => {
      responderLeitura(completo);
      botaoComTexto(textos.salvar)!.click();
      http
        .expectOne(`${base}/funcionamento`)
        .flush({ message: 'UF inexistente' }, { status: 400, statusText: 'Bad Request' });
      fixture.detectChanges();
      expect(raiz.textContent).toContain(textos.erro.salvarTitulo);
      expect(raiz.textContent).toContain('UF inexistente');
      expect(raiz.textContent).not.toContain(textos.sucesso);
      expect(botaoComTexto(textos.salvar)?.disabled).toBeFalse();
    });

    it('valida a data especial entre hoje e hoje mais 180 dias', () => {
      responderLeitura(vazioDto);
      abrirPersonalizar();
      const adicionar = () => botaoComTexto(textos.novo.adicionar)!;
      const data = raiz.querySelector<HTMLInputElement>('#func-nova-data')!;
      expect(data.min).toBe(componente.dataMinima);
      expect(data.max).toBe(componente.dataMaxima);
      expect(adicionar().disabled).toBeTrue();

      digitar('#func-nova-data', somarDias(componente.dataMinima, -1));
      expect(raiz.textContent).toContain(textos.novo.dataErro);
      expect(adicionar().disabled).toBeTrue();

      digitar('#func-nova-data', somarDias(componente.dataMaxima, 1));
      expect(adicionar().disabled).toBeTrue();

      digitar('#func-nova-data', componente.dataMaxima);
      expect(raiz.textContent).not.toContain(textos.novo.dataErro);
      expect(adicionar().disabled).toBeFalse();

      componente.novoMotivo.set('a'.repeat(121));
      fixture.detectChanges();
      expect(adicionar().disabled).toBeTrue();
    });

    it('adicionar dia especial faz POST e mostra o aviso com a data enviada quando ha reservas', () => {
      responderLeitura(vazioDto);
      abrirPersonalizar();
      digitar('#func-nova-data', componente.dataMaxima);
      digitar('#func-novo-motivo', ' Folga ');
      botaoComTexto(textos.novo.adicionar)!.click();
      fixture.detectChanges();
      expect(botaoComTexto(textos.novo.adicionar)?.disabled).toBeTrue();

      const req = http.expectOne(`${base}/dias-especiais`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ data: componente.dataMaxima, fechado: true, motivo: 'Folga' });
      req.flush(
        {
          id: 9,
          data: componente.dataMaxima,
          fechado: true,
          abre: null,
          fecha: null,
          motivo: 'Folga',
          reservasNaData: ['Jogo A', 'Jogo B']
        },
        { status: 201, statusText: 'Created' }
      );
      fixture.detectChanges();
      const aviso = Array.from(raiz.querySelectorAll('[role="alert"]')).find((el) =>
        el.textContent?.includes(textos.aviso.titulo)
      );
      expect(aviso).toBeDefined();
      expect(aviso?.querySelectorAll('li').length).toBe(2);
      expect(aviso?.textContent).toContain(componente.dataCurta(componente.dataMaxima));
      expect(raiz.querySelectorAll('#func-personalizar ul li').length).toBe(3);
    });

    it('dia especial com horario proprio envia abre e fecha e sem reservas nao mostra aviso', () => {
      responderLeitura(vazioDto);
      abrirPersonalizar();
      digitar('#func-nova-data', componente.dataMinima);
      componente.novoFechado.set(false);
      fixture.detectChanges();
      digitar('#func-novo-abre', '10:00');
      digitar('#func-novo-fecha', '16:00');
      botaoComTexto(textos.novo.adicionar)!.click();
      const req = http.expectOne(`${base}/dias-especiais`);
      expect(req.request.body).toEqual({ data: componente.dataMinima, fechado: false, abre: '10:00', fecha: '16:00' });
      req.flush(
        {
          id: 3,
          data: componente.dataMinima,
          fechado: false,
          abre: '10:00',
          fecha: '16:00',
          motivo: null,
          reservasNaData: []
        },
        { status: 201, statusText: 'Created' }
      );
      fixture.detectChanges();
      expect(raiz.textContent).not.toContain(textos.aviso.titulo);
      expect(raiz.querySelectorAll('#func-personalizar ul li').length).toBe(1);
    });

    it('erro 400 ao cadastrar dia especial mostra a mensagem do servidor', () => {
      responderLeitura(vazioDto);
      abrirPersonalizar();
      digitar('#func-nova-data', componente.dataMinima);
      botaoComTexto(textos.novo.adicionar)!.click();
      http.expectOne(`${base}/dias-especiais`).flush('Data repetida', { status: 400, statusText: 'Bad Request' });
      fixture.detectChanges();
      expect(raiz.textContent).toContain(textos.erro.diaTitulo);
      expect(raiz.textContent).toContain('Data repetida');
      expect(raiz.querySelectorAll('#func-personalizar ul li').length).toBe(0);
    });

    it('Remover faz DELETE e tira o dia da lista', () => {
      responderLeitura(completo);
      expect(raiz.querySelectorAll('#func-personalizar ul li').length).toBe(2);
      raiz.querySelector<HTMLButtonElement>('#func-personalizar ul li button')!.click();
      fixture.detectChanges();
      const req = http.expectOne(`${base}/dias-especiais/2`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();
      expect(raiz.querySelectorAll('#func-personalizar ul li').length).toBe(1);
      expect(raiz.textContent).not.toContain('Feriado municipal');
    });
  });

  describe('sem a sessao da empresa', () => {
    beforeEach(() => montar(null));

    it('vai direto para o erro sem chamar a API', () => {
      expect(componente.estado()).toBe('erro');
      expect(raiz.textContent).toContain(textos.erro.titulo);
    });
  });
});
