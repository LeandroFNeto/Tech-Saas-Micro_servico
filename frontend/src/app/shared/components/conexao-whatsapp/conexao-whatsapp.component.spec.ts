import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { CodigoPareamentoResponseDTO, SessaoStatusResponseDTO } from '../../models/whatsapp.models';
import {
  AVISO_CODIGO_ULTIMOS_SEGUNDOS,
  ConexaoWhatsappComponent,
  ESPERA_REPETICAO_409_MS,
  GUARDA_DESCONECTADO_MS,
  INTERVALO_STATUS_LENTO_MS,
  INTERVALO_STATUS_MS,
  REPETICOES_409,
  VALIDADE_CODIGO_SEGUNDOS
} from './conexao-whatsapp.component';

const SESSAO = 'sessao_exemplo_01';
const TELEFONE_FALSO = '5511900000000';
const CODIGO_FALSO = 'ABCD-EFGH';

function dto(status: SessaoStatusResponseDTO['status'], extra: Partial<SessaoStatusResponseDTO> = {}): SessaoStatusResponseDTO {
  return { sessao: SESSAO, status, qrcodeBase64: null, ...extra };
}

function erroHttp(status: number): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { message: 'texto cru da API' } });
}

describe('ConexaoWhatsappComponent', () => {
  let fixture: ComponentFixture<ConexaoWhatsappComponent>;
  let component: ConexaoWhatsappComponent;
  let apiSpy: jasmine.SpyObj<WhatsappConnectionService>;
  let statusAtual: SessaoStatusResponseDTO;

  let descritorClipboard: PropertyDescriptor | undefined;

  beforeEach(() => {
    descritorClipboard = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
    statusAtual = dto('DISCONNECTED');
    apiSpy = jasmine.createSpyObj<WhatsappConnectionService>('WhatsappConnectionService', [
      'consultarStatus',
      'iniciar',
      'solicitarCodigoPareamento'
    ]);
    apiSpy.consultarStatus.and.callFake(() => of(statusAtual));
    apiSpy.iniciar.and.callFake(() => of(dto('QRCODE')));
    apiSpy.solicitarCodigoPareamento.and.callFake(() => of({ sessao: SESSAO, codigo: CODIGO_FALSO }));

    TestBed.configureTestingModule({
      imports: [ConexaoWhatsappComponent],
      providers: [{ provide: WhatsappConnectionService, useValue: apiSpy }]
    });
  });

  afterEach(() => {
    if (descritorClipboard) {
      Object.defineProperty(navigator, 'clipboard', descritorClipboard);
    } else {
      delete (navigator as unknown as Record<string, unknown>)['clipboard'];
    }
  });

  function detectar(ms = 0): void {
    fixture.detectChanges();
    tick(ms);
    fixture.detectChanges();
    flushMicrotasks();
    fixture.detectChanges();
  }

  function abrir(opcoes: { compacto?: boolean; sessao?: string } = {}): void {
    fixture = TestBed.createComponent(ConexaoWhatsappComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('sessao', opcoes.sessao ?? SESSAO);
    fixture.componentRef.setInput('compacto', opcoes.compacto ?? false);
    detectar();
  }

  function raiz(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function classesDe(alvo: Element | string | null): string[] {
    const bruto = typeof alvo === 'string' ? alvo : (alvo?.className ?? '');
    return bruto.split(/\s+/).filter(Boolean).sort();
  }

  function texto(): string {
    return (raiz().textContent ?? '').replace(/\s+/g, ' ');
  }

  function botao(rotulo: string): HTMLButtonElement | undefined {
    return Array.from(raiz().querySelectorAll('button')).find((b) => (b.textContent ?? '').includes(rotulo));
  }

  function clicar(rotulo: string): void {
    const alvo = botao(rotulo);
    expect(alvo).withContext(`botao "${rotulo}"`).toBeDefined();
    alvo!.click();
    detectar();
  }

  function digitar(valor: string): void {
    const campo = raiz().querySelector('input') as HTMLInputElement;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    detectar();
  }

  function enviarFormulario(): void {
    const form = raiz().querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    detectar();
  }

  function irParaCodigo(telefone = '+55 (11) 90000-0000'): void {
    if (statusAtual.status === 'DISCONNECTED') {
      statusAtual = dto('QRCODE');
    }
    clicar('Conectar com número de telefone');
    digitar(telefone);
    enviarFormulario();
  }

  function irParaCodigoDoTelefone(): void {
    digitar('+55 (11) 90000-0000');
    enviarFormulario();
  }

  function encerrar(): void {
    fixture.destroy();
    discardPeriodicTasks();
  }

  describe('WPP-24 conexão guiada por código', () => {
    it('Conectar pelo código: inicia a sessão, pede o código e mostra instruções até ficar online', fakeAsync(() => {
      abrir();
      expect(texto()).toContain('Conectar com número de telefone');

      irParaCodigo();

      expect(apiSpy.iniciar).toHaveBeenCalledOnceWith(SESSAO);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledOnceWith(SESSAO, TELEFONE_FALSO);
      expect(component.passo()).toBe('codigo');
      expect(raiz().querySelector('[data-teste="codigo"]')?.textContent).toContain(CODIGO_FALSO);
      expect(texto()).toContain('Toque em Aparelhos conectados');
      expect(texto()).toContain('depois em Conectar com número de telefone.');
      expect(texto()).toContain('Aguardando você digitar o código no celular');
      expect(texto()).toContain('Vale por mais');

      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);
      expect(texto()).toContain('WhatsApp online');
      expect(component.passo()).toBe('conectado');
      encerrar();
    }));

    it('Telefone inválido: mostra a mensagem do campo e não chama a API', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');
      digitar('11 99999');
      enviarFormulario();

      expect(texto()).toContain('Confira o número: DDD e número, com 12 ou 13 dígitos contando o 55.');
      expect(raiz().querySelector('input')?.getAttribute('aria-invalid')).toBe('true');
      expect(apiSpy.iniciar).not.toHaveBeenCalled();
      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      expect(component.passo()).toBe('telefone');
      encerrar();
    }));

    it('Telefone colado com símbolos: usa só os dígitos e valida o resultado', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');
      digitar('+55 11 99999.9999');

      expect((raiz().querySelector('input') as HTMLInputElement).value).toBe('+55 (11) 99999-9999');
      enviarFormulario();
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledOnceWith(SESSAO, '5511999999999');
      encerrar();
    }));

    it('digitar de novo depois do erro de telefone tira a mensagem do campo', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');
      digitar('1');
      enviarFormulario();
      expect(component.erro()).toBe('400');

      digitar('11');
      expect(component.erro()).toBeNull();
      encerrar();
    }));

    it('Copiar o código: manda para a área de transferência e mostra Copiado por 2 s', fakeAsync(() => {
      const escrever = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', { value: { writeText: escrever }, configurable: true });
      abrir();
      irParaCodigo();

      clicar('Copiar');
      expect(escrever).toHaveBeenCalledOnceWith(CODIGO_FALSO);
      expect(texto()).toContain('Copiado');

      detectar(1999);
      expect(texto()).toContain('Copiado');
      detectar(1);
      expect(texto()).not.toContain('Copiado');
      expect(botao('Copiar')).toBeDefined();
      encerrar();
    }));

    it('Copiar o código: dois cliques seguidos reiniciam os 2 s do Copiado', fakeAsync(() => {
      const escrever = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', { value: { writeText: escrever }, configurable: true });
      abrir();
      irParaCodigo();

      clicar('Copiar');
      detectar(1000);
      clicar('Copiado');
      expect(escrever).toHaveBeenCalledTimes(2);

      detectar(1500);
      expect(texto()).toContain('Copiado');
      detectar(500);
      expect(texto()).not.toContain('Copiado');
      encerrar();
    }));

    it('Copiar o código: se a área de transferência recusar, o botão segue em Copiar', fakeAsync(() => {
      const escrever = jasmine.createSpy('writeText').and.callFake(() => Promise.reject(new Error('negado')));
      Object.defineProperty(navigator, 'clipboard', { value: { writeText: escrever }, configurable: true });
      abrir();
      irParaCodigo();

      clicar('Copiar');
      expect(component.copiado()).toBeFalse();
      expect(texto()).not.toContain('Copiado');
      encerrar();
    }));

    it('copiar sem código na tela não chama a área de transferência', fakeAsync(() => {
      const escrever = jasmine.createSpy('writeText').and.returnValue(Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', { value: { writeText: escrever }, configurable: true });
      abrir();

      void component.copiar();
      flushMicrotasks();

      expect(escrever).not.toHaveBeenCalled();
      encerrar();
    }));

    it('Contagem de validade: mostra o tempo e diminui até zero', fakeAsync(() => {
      abrir();
      irParaCodigo();
      expect(raiz().querySelector('[data-teste="tempo"]')?.textContent).toBe('2:00');

      detectar(1000);
      expect(raiz().querySelector('[data-teste="tempo"]')?.textContent).toBe('1:59');
      detectar(59_000);
      expect(raiz().querySelector('[data-teste="tempo"]')?.textContent).toBe('1:00');
      expect(component.porcentagemRestante()).toBe(50);
      expect(component.nosUltimosSegundos()).toBeFalse();
      detectar((VALIDADE_CODIGO_SEGUNDOS - 60 - AVISO_CODIGO_ULTIMOS_SEGUNDOS) * 1000);
      expect(component.nosUltimosSegundos()).toBeTrue();
      encerrar();
    }));

    it('Código expirado: a contagem chega a zero e a tela oferece Gerar outro código com o QR Code como alternativa', fakeAsync(() => {
      abrir();
      irParaCodigo();

      detectar(VALIDADE_CODIGO_SEGUNDOS * 1000);

      expect(component.passo()).toBe('codigo-expirado');
      expect(texto()).toContain('O código expirou');
      expect(botao('Gerar outro código')).toBeDefined();
      expect(botao('Prefiro ler o QR Code')).toBeDefined();
      expect(component.segundosRestantes()).toBe(0);
      encerrar();
    }));

    it('Gerar outro código: pede um novo, substitui o anterior e reinicia a contagem', fakeAsync(() => {
      abrir();
      irParaCodigo();
      detectar(30_000);
      apiSpy.solicitarCodigoPareamento.and.callFake(() => of({ sessao: SESSAO, codigo: 'WXYZ-1234' }));

      clicar('Gerar outro código');

      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(2);
      expect(apiSpy.solicitarCodigoPareamento.calls.mostRecent().args).toEqual([SESSAO, TELEFONE_FALSO]);
      expect(raiz().querySelector('[data-teste="codigo"]')?.textContent).toContain('WXYZ-1234');
      expect(component.segundosRestantes()).toBe(VALIDADE_CODIGO_SEGUNDOS);
      encerrar();
    }));

    it('Gerar outro código a partir do código expirado volta ao passo do código', fakeAsync(() => {
      abrir();
      irParaCodigo();
      detectar(VALIDADE_CODIGO_SEGUNDOS * 1000);
      expect(component.passo()).toBe('codigo-expirado');

      clicar('Gerar outro código');

      expect(component.passo()).toBe('codigo');
      expect(component.segundosRestantes()).toBe(VALIDADE_CODIGO_SEGUNDOS);
      encerrar();
    }));

    it('gerar outro código sem telefone guardado não chama a API', fakeAsync(() => {
      abrir();

      component.gerarOutroCodigo();

      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      encerrar();
    }));

    it('Clique duplo: só um pedido e o segundo clique não consome o limite', fakeAsync(() => {
      const pendente = new Subject<SessaoStatusResponseDTO>();
      apiSpy.iniciar.and.returnValue(pendente.asObservable());
      abrir();
      clicar('Conectar com número de telefone');
      digitar('+55 (11) 90000-0000');

      component.gerarCodigo();
      component.gerarCodigo();
      detectar();

      expect(apiSpy.iniciar).toHaveBeenCalledTimes(1);
      expect((raiz().querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBeTrue();
      pendente.next(dto('QRCODE'));
      pendente.complete();
      detectar();
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(1);
      encerrar();
    }));

    it('WhatsApp recusou o código: status segue QRCODE ou DISCONNECTED dentro da guarda, a tela fica no código, a contagem corre e dá para gerar outro', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('QRCODE');
      detectar(6000);
      expect(component.passo()).toBe('codigo');

      statusAtual = dto('DISCONNECTED');
      detectar(38_900);
      expect(component.passo()).toBe('codigo');
      expect(component.segundosRestantes()).toBe(76);
      expect(botao('Gerar outro código')).toBeDefined();
      expect(botao('Gerar outro código')?.disabled).toBeFalse();

      detectar(1_600);
      expect(component.passo()).toBe('codigo');
      encerrar();
    }));

    it('limites da especificação: 120 s de validade, 3 repetições a cada 2 s, guarda de 45 s e polling de 1,5 s e 5 s', () => {
      expect(VALIDADE_CODIGO_SEGUNDOS).toBe(120);
      expect(REPETICOES_409).toBe(3);
      expect(ESPERA_REPETICAO_409_MS).toBe(2000);
      expect(GUARDA_DESCONECTADO_MS).toBe(45_000);
      expect(INTERVALO_STATUS_MS).toBe(1500);
      expect(INTERVALO_STATUS_LENTO_MS).toBe(5000);
    });

    it('Gerar outro código fica habilitado assim que o código aparece', fakeAsync(() => {
      abrir();
      irParaCodigo();
      expect(botao('Gerar outro código')).toBeDefined();
      expect(botao('Gerar outro código')?.disabled).toBeFalse();
      encerrar();
    }));

    it('400 ao gerar outro código no passo do código mostra a mensagem de número', fakeAsync(() => {
      abrir();
      irParaCodigo();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(400)));

      clicar('Gerar outro código');

      expect(component.passo()).toBe('codigo');
      expect(texto()).toContain(component.conteudo.erros['400']);
      encerrar();
    }));

    it('400 ao gerar outro código no passo do código expirado mostra a mensagem de número', fakeAsync(() => {
      abrir();
      irParaCodigo();
      detectar(VALIDADE_CODIGO_SEGUNDOS * 1000);
      expect(component.passo()).toBe('codigo-expirado');
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(400)));

      clicar('Gerar outro código');

      expect(component.passo()).toBe('codigo-expirado');
      expect(texto()).toContain(component.conteudo.erros['400']);
      encerrar();
    }));

    it('Muitos pedidos: 429 mostra a espera e oferece o QR Code', fakeAsync(() => {
      abrir();
      irParaCodigo();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(429)));

      clicar('Gerar outro código');

      expect(texto()).toContain('Muitos pedidos.');
      expect(texto()).toContain('Aguarde alguns minutos ou use o QR Code.');
      expect(texto()).not.toContain('texto cru da API');
      clicar('Usar o QR Code');
      expect(component.passo()).toBe('qrcode');
      encerrar();
    }));

    it('Já conectado: 409 e status CONNECTED mostram WhatsApp online', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(409)));
      statusAtual = dto('CONNECTED');
      clicar('Conectar com número de telefone');
      digitar('+55 (11) 90000-0000');
      enviarFormulario();

      expect(component.passo()).toBe('conectado');
      expect(texto()).toContain('WhatsApp online');
      expect(component.gerando()).toBeFalse();
      encerrar();
    }));

    it('Conexão ainda iniciando: repete até 3 vezes a cada 2 s e depois oferece Tentar de novo', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(409)));
      statusAtual = dto('QRCODE');
      clicar('Conectar com número de telefone');
      digitar('+55 (11) 90000-0000');
      enviarFormulario();

      const preparando = (): void => {
        expect(texto()).toContain(component.conteudo.erros['409Preparando']);
        expect(component.gerando()).toBeTrue();
        expect((raiz().querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBeTrue();
      };
      preparando();
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(1);

      detectar(1999);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(1);
      detectar(1);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(2);
      preparando();
      detectar(2000);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(3);
      preparando();
      detectar(2000);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(4);

      expect(texto()).toContain('A conexão ainda está iniciando.');
      expect(texto()).toContain('Tente de novo em alguns segundos.');
      expect(component.gerando()).toBeFalse();

      detectar(6000);
      expect(apiSpy.solicitarCodigoPareamento).toHaveBeenCalledTimes(4);

      apiSpy.solicitarCodigoPareamento.and.callFake(() => of({ sessao: SESSAO, codigo: CODIGO_FALSO }));
      clicar('Tentar de novo');
      expect(component.passo()).toBe('codigo');
      expect(apiSpy.iniciar).toHaveBeenCalledTimes(1);
      encerrar();
    }));

    it('Conexão ainda iniciando: a repetição automática que dá certo mostra o código', fakeAsync(() => {
      abrir();
      let chamadas = 0;
      apiSpy.solicitarCodigoPareamento.and.callFake((): Observable<CodigoPareamentoResponseDTO> => {
        chamadas++;
        return chamadas === 1 ? throwError(() => erroHttp(409)) : of({ sessao: SESSAO, codigo: CODIGO_FALSO });
      });
      statusAtual = dto('QRCODE');
      irParaCodigo();
      expect(texto()).toContain('Preparando a conexão…');

      detectar(2000);

      expect(component.passo()).toBe('codigo');
      expect(texto()).not.toContain('Preparando a conexão…');
      encerrar();
    }));

    it('Conexão ainda iniciando: se a consulta do status falhar, mostra o aviso com Tentar de novo', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(409)));
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(500)));
      irParaCodigo();

      expect(component.erro()).toBe('409Iniciando');
      expect(botao('Tentar de novo')).toBeDefined();
      encerrar();
    }));

    it('Sessão de outra empresa: 403 mostra a mensagem e volta ao início', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(403)));
      irParaCodigo();

      expect(texto()).toContain('Você não tem acesso a esta conexão.');
      expect(texto()).not.toContain('texto cru da API');
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('Empresa sem sessão: 404 mostra a mensagem e volta ao início', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(404)));
      irParaCodigo();

      expect(texto()).toContain(component.conteudo.erros['404']);
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('Sessão do painel expirada: 401 limpa telefone, código e contagem', fakeAsync(() => {
      abrir();
      irParaCodigo();
      expect(component.codigo()).toBe(CODIGO_FALSO);
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(401)));

      clicar('Gerar outro código');

      expect(component.passo()).toBe('inicio');
      expect(component.codigo()).toBe('');
      expect(component.telefone()).toBe('');
      expect(component.segundosRestantes()).toBe(0);
      expect(component.erro()).toBeNull();
      expect(texto()).not.toContain(CODIGO_FALSO);

      apiSpy.solicitarCodigoPareamento.calls.reset();
      component.gerarOutroCodigo();
      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      encerrar();
    }));

    it('401 ao consultar o status também limpa a tela e para o polling', fakeAsync(() => {
      abrir();
      irParaCodigo();
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(401)));
      detectar(INTERVALO_STATUS_MS);
      const chamadas = apiSpy.consultarStatus.calls.count();

      expect(component.codigo()).toBe('');
      expect(component.passo()).toBe('inicio');
      detectar(6000);
      expect(apiSpy.consultarStatus.calls.count()).toBe(chamadas);
      encerrar();
    }));

    it('401 ao consultar o status com pedido de código em andamento: o código que chega depois não aparece', fakeAsync(() => {
      abrir();
      irParaCodigo();
      const pendente = new Subject<CodigoPareamentoResponseDTO>();
      apiSpy.solicitarCodigoPareamento.and.returnValue(pendente.asObservable());
      clicar('Gerar outro código');
      expect(component.gerando()).toBeTrue();

      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(401)));
      detectar(1500);
      expect(component.passo()).toBe('inicio');

      pendente.next({ sessao: SESSAO, codigo: 'ZZZZ-9999' });
      pendente.complete();
      detectar();

      expect(component.codigo()).toBe('');
      expect(component.passo()).toBe('inicio');
      expect(texto()).not.toContain('ZZZZ-9999');
      apiSpy.solicitarCodigoPareamento.calls.reset();
      component.gerarOutroCodigo();
      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      encerrar();
    }));

    it('troca de empresa com pedido de código em andamento: o código da empresa anterior não aparece na nova', fakeAsync(() => {
      abrir();
      irParaCodigo();
      const pendente = new Subject<CodigoPareamentoResponseDTO>();
      apiSpy.solicitarCodigoPareamento.and.returnValue(pendente.asObservable());
      clicar('Gerar outro código');
      expect(component.gerando()).toBeTrue();

      fixture.componentRef.setInput('sessao', 'sessao_exemplo_02');
      detectar();
      expect(component.passo()).toBe('inicio');

      pendente.next({ sessao: SESSAO, codigo: 'ZZZZ-9999' });
      pendente.complete();
      detectar();

      expect(component.codigo()).toBe('');
      expect(component.passo()).toBe('inicio');
      expect(texto()).not.toContain('ZZZZ-9999');
      apiSpy.solicitarCodigoPareamento.calls.reset();
      component.gerarOutroCodigo();
      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      encerrar();
    }));

    it('Código indisponível: 503 abre o QR Code como saída, sem voltar ao código', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(503)));
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });
      irParaCodigo();

      expect(component.passo()).toBe('qrcode');
      expect(texto()).toContain(component.conteudo.erros['503']);
      expect(texto()).not.toContain('Voltar para o código');
      detectar(INTERVALO_STATUS_MS);
      expect(raiz().querySelector('img')).not.toBeNull();
      encerrar();
    }));

    it('erro inesperado mostra a mensagem genérica e não o texto da API', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(500)));
      irParaCodigo();

      expect(texto()).toContain(component.conteudo.erros.generico);
      expect(texto()).not.toContain('texto cru da API');
      expect(component.gerando()).toBeFalse();
      encerrar();
    }));

    it('400 devolvido pela API fica no passo do telefone com a mensagem do campo', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => erroHttp(400)));
      irParaCodigo();

      expect(component.passo()).toBe('telefone');
      expect(texto()).toContain('Confira o número');
      encerrar();
    }));

    it('falha ao iniciar a sessão mostra o erro e libera o botão', fakeAsync(() => {
      abrir();
      apiSpy.iniciar.and.returnValue(throwError(() => erroHttp(403)));
      irParaCodigo();

      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      expect(texto()).toContain(component.conteudo.erros['403']);
      expect(component.gerando()).toBeFalse();
      encerrar();
    }));

    it('sessão já conectada ao iniciar pula o código e mostra online', fakeAsync(() => {
      abrir();
      apiSpy.iniciar.and.returnValue(of(dto('CONNECTED')));
      statusAtual = dto('CONNECTED');
      irParaCodigo();

      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      expect(component.passo()).toBe('conectado');
      encerrar();
    }));

    it('QR Code como alternativa: mostra o QR e o polling segue até CONNECTED', fakeAsync(() => {
      abrir();
      apiSpy.iniciar.and.returnValue(of(dto('QRCODE', { qrcodeBase64: 'imagem_qr' })));
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });

      clicar('Prefiro ler o QR Code');

      expect(component.passo()).toBe('qrcode');
      expect(raiz().querySelector('img')?.getAttribute('src')).toBe('data:image/png;base64,imagem_qr');
      expect(texto()).toContain('Voltar para o código');
      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);
      expect(texto()).toContain('WhatsApp online');
      encerrar();
    }));

    it('QR Code sem imagem ainda mostra QR Code a caminho', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE');
      clicar('Prefiro ler o QR Code');

      expect(texto()).toContain('QR Code a caminho. Mantenha esta tela aberta.');
      encerrar();
    }));

    it('QR Code: falha ao iniciar mostra o erro e libera o botão', fakeAsync(() => {
      abrir();
      apiSpy.iniciar.and.returnValue(throwError(() => erroHttp(404)));

      clicar('Prefiro ler o QR Code');

      expect(texto()).toContain(component.conteudo.erros['404']);
      expect(component.iniciando()).toBeFalse();
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('usarQrcode ignora o segundo clique enquanto inicia e sem sessão', fakeAsync(() => {
      const pendente = new Subject<SessaoStatusResponseDTO>();
      apiSpy.iniciar.and.returnValue(pendente.asObservable());
      abrir();

      component.usarQrcode();
      component.usarQrcode();
      expect(apiSpy.iniciar).toHaveBeenCalledTimes(1);
      pendente.next(dto('QRCODE'));
      pendente.complete();
      encerrar();

      abrir({ sessao: '' });
      component.usarQrcode();
      expect(apiSpy.iniciar).toHaveBeenCalledTimes(1);
      encerrar();
    }));

    it('Voltar para o código: do QR Code volta ao passo do telefone com o campo vazio', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE');
      clicar('Prefiro ler o QR Code');

      clicar('Voltar para o código');

      expect(component.passo()).toBe('telefone');
      expect((raiz().querySelector('input') as HTMLInputElement).value).toBe('');
      encerrar();
    }));

    it('Voltar no passo do telefone retorna ao início', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');

      clicar('Voltar');

      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('Telefone e código fora do armazenamento: nada em storage, URL, título nem log, e limpos ao sair do passo', fakeAsync(() => {
      const gravarLocal = spyOn(Storage.prototype, 'setItem').and.callThrough();
      const log = spyOn(console, 'log');
      const aviso = spyOn(console, 'warn');
      const erroLog = spyOn(console, 'error');
      const titulo = document.title;
      const endereco = location.href;
      abrir();
      irParaCodigo();

      expect(gravarLocal).not.toHaveBeenCalled();
      expect(document.title).toBe(titulo);
      expect(document.title).not.toContain(CODIGO_FALSO);
      expect(location.href).toBe(endereco);
      expect(location.href).not.toContain(TELEFONE_FALSO);
      expect(log).not.toHaveBeenCalled();
      expect(aviso).not.toHaveBeenCalled();
      expect(erroLog).not.toHaveBeenCalled();
      expect(component.telefone()).toBe('');

      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);
      expect(component.codigo()).toBe('');
      expect(texto()).not.toContain(CODIGO_FALSO);
      encerrar();
    }));

    it('Troca de empresa no admin: volta ao início e limpa telefone, código e contagem', fakeAsync(() => {
      abrir();
      irParaCodigo();
      expect(component.passo()).toBe('codigo');

      fixture.componentRef.setInput('sessao', 'sessao_exemplo_02');
      detectar();

      expect(component.passo()).toBe('inicio');
      expect(component.codigo()).toBe('');
      expect(component.telefone()).toBe('');
      expect(component.segundosRestantes()).toBe(0);
      expect(apiSpy.consultarStatus).toHaveBeenCalledWith('sessao_exemplo_02');
      apiSpy.solicitarCodigoPareamento.calls.reset();
      component.gerarOutroCodigo();
      expect(apiSpy.solicitarCodigoPareamento).not.toHaveBeenCalled();
      detectar(120_000);
      expect(component.segundosRestantes()).toBe(0);
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('regressao: página recarregada abre no início com o estado do servidor e sem herdar o código anterior', fakeAsync(() => {
      abrir();
      irParaCodigo();
      encerrar();

      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });
      abrir();

      expect(component.passo()).toBe('inicio');
      expect(texto()).not.toContain(CODIGO_FALSO);
      expect(botao('Conectar com número de telefone')).toBeDefined();
      encerrar();
    }));

    it('Aba em segundo plano: polling lento e consulta na hora ao voltar', fakeAsync(() => {
      abrir();
      const oculta = spyOnProperty(document, 'hidden', 'get').and.returnValue(true);
      document.dispatchEvent(new Event('visibilitychange'));
      detectar();
      const antes = apiSpy.consultarStatus.calls.count();

      detectar(4900);
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes);
      detectar(100);
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes + 1);

      const durante = apiSpy.consultarStatus.calls.count();
      oculta.and.returnValue(false);
      document.dispatchEvent(new Event('visibilitychange'));
      tick(0);
      expect(apiSpy.consultarStatus.calls.count()).toBe(durante + 1);
      encerrar();
    }));

    it('Aba visível de novo: a contagem é recalculada pelo relógio na hora, sem esperar o próximo segundo', fakeAsync(() => {
      abrir();
      irParaCodigo();
      const oculta = spyOnProperty(document, 'hidden', 'get').and.returnValue(true);
      document.dispatchEvent(new Event('visibilitychange'));
      const base = Date.now();
      spyOn(Date, 'now').and.returnValue(base + 10_000);
      expect(component.segundosRestantes()).toBe(120);

      oculta.and.returnValue(false);
      document.dispatchEvent(new Event('visibilitychange'));

      expect(component.segundosRestantes()).toBe(110);
      encerrar();
    }));

    it('Navegador sem internet: avisa, pausa o polling e retoma ao voltar', fakeAsync(() => {
      abrir();
      window.dispatchEvent(new Event('offline'));
      detectar();
      expect(texto()).toContain('Sem internet neste aparelho.');
      expect(texto()).toContain('Vamos tentar de novo quando voltar.');
      const antes = apiSpy.consultarStatus.calls.count();

      detectar(INTERVALO_STATUS_MS * 5);
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes);

      window.dispatchEvent(new Event('online'));
      detectar();
      expect(texto()).not.toContain('Sem internet neste aparelho.');
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes + 1);
      encerrar();
    }));

    it('Navegador já sem internet ao abrir a tela mostra o aviso', fakeAsync(() => {
      spyOnProperty(navigator, 'onLine', 'get').and.returnValue(false);
      abrir();

      expect(component.semInternet()).toBeTrue();
      expect(apiSpy.consultarStatus).not.toHaveBeenCalled();
      encerrar();
    }));

    it('Acessibilidade do código: foco no código, anúncio educado, rótulos e alvos de toque de 44 px', fakeAsync(() => {
      abrir();
      irParaCodigo();
      detectar();

      const codigo = raiz().querySelector('[data-teste="codigo"]') as HTMLElement;
      expect(document.activeElement).toBe(codigo);
      expect(codigo.getAttribute('aria-live')).toBe('polite');
      expect(codigo.className).toContain('select-all');
      expect(botao('Copiar')?.getAttribute('aria-label')).toBe('Copiar o código');
      expect(botao('Copiar')?.className).toContain('min-h-11');
      expect(botao('Gerar outro código')?.className).toContain('min-h-12');
      expect(botao('Prefiro ler o QR Code')?.className).toContain('min-h-11');
      expect(raiz().querySelector('[data-teste="instrucoes"]')?.getAttribute('aria-label')).toBe(component.conteudo.codigo.instrucoesRotulo);
      encerrar();
    }));

    it('Acessibilidade: o campo do telefone tem rótulo e dica ligados', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');

      const campo = raiz().querySelector('input') as HTMLInputElement;
      expect(raiz().querySelector(`label[for="${campo.id}"]`)?.textContent).toContain('Número do WhatsApp da empresa');
      const dica = raiz().querySelector(`#${campo.getAttribute('aria-describedby')}`);
      expect(dica?.textContent).toContain('O número do celular onde está o WhatsApp, com DDD e o 9.');
      encerrar();
    }));

    it('Acessibilidade: a contagem só é anunciada em 1:00 e 0:30', fakeAsync(() => {
      abrir();
      irParaCodigo();
      const anuncio = () => raiz().querySelector('[data-teste="anuncio"]')?.textContent;

      detectar(30_000);
      expect(anuncio()).toBe('');
      detectar(30_000);
      expect(anuncio()).toBe('Falta 1 minuto para o código expirar.');
      detectar(30_000);
      expect(anuncio()).toBe('Faltam 30 segundos para o código expirar.');
      detectar(30_000);
      expect(anuncio() ?? '').toBe('');
      encerrar();
    }));

    it('Versões da tela: compacto mostra os mesmos passos em layout reduzido e a versão normal mostra a lista completa', fakeAsync(() => {
      abrir({ compacto: true });
      irParaCodigo();
      expect(component.compacto).toBeTrue();
      expect(texto()).toContain('Passo 2 de 3 · Código');
      expect(raiz().querySelector('[data-teste="etapas"]')).toBeNull();
      expect(texto()).toContain('No celular: WhatsApp → Aparelhos conectados');
      expect(raiz().querySelector('ol')).toBeNull();
      expect(texto()).not.toContain(component.conteudo.cabecalho.descricao);
      expect(classesDe(raiz().querySelector('section'))).toEqual(classesDe(component.design.cartao.compacto));
      encerrar();

      abrir({ compacto: false });
      irParaCodigo();
      expect(raiz().querySelectorAll('[data-teste="etapas"] li:not([aria-hidden="true"])').length).toBe(3);
      expect(raiz().querySelectorAll('[data-teste="instrucoes"] li').length).toBe(4);
      expect(texto()).toContain(component.conteudo.cabecalho.descricao);
      expect(classesDe(raiz().querySelector('section'))).toEqual(classesDe(component.design.cartao.normal));
      encerrar();
    }));

    it('texto do início muda entre as versões normal e compacta', fakeAsync(() => {
      abrir({ compacto: true });
      expect(texto()).toContain('Conecte com um código digitado no próprio celular.');
      encerrar();

      abrir({ compacto: false });
      expect(texto()).toContain('Você recebe um código de 8 caracteres');
      encerrar();
    }));

    it('etapas: o passo conectando marca número e código como concluídos', fakeAsync(() => {
      abrir();
      irParaCodigo();
      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_MS);

      expect(component.etapaAtual()).toBe(3);
      expect(component.mostraEtapas()).toBeTrue();
      const numeros = Array.from(
        raiz().querySelectorAll('[data-teste="etapas"] li:not([aria-hidden="true"]) > span:first-child')
      ).map((el) => (el.textContent ?? '').trim());
      expect(numeros).toEqual(['✓', '✓', '3']);
      encerrar();
    }));

    it('textoEtapaCompacta: Passo 1 de 3 no telefone e Passo 3 de 3 ao conectar', fakeAsync(() => {
      abrir({ compacto: true });
      clicar('Conectar com número de telefone');
      expect(component.textoEtapaCompacta()).toBe('Passo 1 de 3 · Número');
      expect(texto()).toContain('Passo 1 de 3 · Número');

      irParaCodigoDoTelefone();
      expect(component.textoEtapaCompacta()).toBe('Passo 2 de 3 · Código');

      statusAtual = dto('INITIALIZING');
      detectar(1500);
      expect(component.textoEtapaCompacta()).toBe('Passo 3 de 3 · Conectar');
      expect(texto()).toContain('Passo 3 de 3 · Conectar');
      encerrar();
    }));

    it('ouvintes de ambiente saem ao destruir: online, offline e visibilitychange depois disso não fazem nada', fakeAsync(() => {
      abrir();
      encerrar();
      const ritmo = spyOn(component as unknown as { atualizarRitmo: () => void }, 'atualizarRitmo');

      window.dispatchEvent(new Event('offline'));
      window.dispatchEvent(new Event('online'));
      document.dispatchEvent(new Event('visibilitychange'));

      expect(ritmo).not.toHaveBeenCalled();
      expect(component.semInternet()).toBeFalse();
    }));

    it('sem sessão definida o início não deixa conectar', fakeAsync(() => {
      abrir({ sessao: '' });

      expect(texto()).toContain('Sessão não definida');
      expect(botao('Conectar com número de telefone')?.disabled).toBeTrue();
      component.gerarCodigo();
      expect(apiSpy.iniciar).not.toHaveBeenCalled();
      expect(apiSpy.consultarStatus).not.toHaveBeenCalled();
      encerrar();
    }));

    it('mostra que está consultando enquanto o primeiro status não chega', fakeAsync(() => {
      const pendente = new Subject<SessaoStatusResponseDTO>();
      apiSpy.consultarStatus.and.returnValue(pendente.asObservable());
      abrir();

      expect(texto()).toContain('Consultando status da conexão...');
      expect(botao('Conectar com número de telefone')).toBeUndefined();
      pendente.next(dto('DISCONNECTED'));
      detectar();
      expect(texto()).not.toContain('Consultando status da conexão...');
      expect(botao('Conectar com número de telefone')).toBeDefined();
      encerrar();
    }));

    it('formata srcQrcode com prefixo data uri quando necessário', () => {
      fixture = TestBed.createComponent(ConexaoWhatsappComponent);
      component = fixture.componentInstance;

      component.qrcode.set(null);
      expect(component.srcQrcode()).toBeNull();
      component.qrcode.set('data:image/png;base64,abc');
      expect(component.srcQrcode()).toBe('data:image/png;base64,abc');
      component.qrcode.set('rawbase64string');
      expect(component.srcQrcode()).toBe('data:image/png;base64,rawbase64string');
    });

    it('erro permanente 403 ou 404 ao consultar interrompe o polling e mostra a mensagem do design', fakeAsync(() => {
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(403)));
      abrir();

      expect(apiSpy.consultarStatus).toHaveBeenCalledTimes(1);
      expect(texto()).toContain(component.conteudo.erros['403']);
      detectar(6000);
      expect(apiSpy.consultarStatus).toHaveBeenCalledTimes(1);
      encerrar();

      apiSpy.consultarStatus.calls.reset();
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(404)));
      abrir();
      expect(texto()).toContain(component.conteudo.erros['404']);
      encerrar();
    }));

    it('erro passageiro ao consultar mantém o polling e a tela', fakeAsync(() => {
      abrir();
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(500)));
      detectar(INTERVALO_STATUS_MS);
      const antes = apiSpy.consultarStatus.calls.count();

      detectar(INTERVALO_STATUS_MS * 2);

      expect(apiSpy.consultarStatus.calls.count()).toBeGreaterThan(antes);
      expect(component.passo()).toBe('inicio');
      expect(component.carregando()).toBeFalse();
      encerrar();
    }));

    it('erro sem HttpErrorResponse também é tratado pelo código numérico', fakeAsync(() => {
      abrir();
      apiSpy.solicitarCodigoPareamento.and.returnValue(throwError(() => ({ status: 429 })));
      irParaCodigo();

      expect(component.erro()).toBe('429');
      encerrar();
    }));
  });

  describe('WPP-22 estados da conexão na tela', () => {
    it('Reconectando: INITIALIZING sem passo de pareamento mostra Reconectando e nenhum botão de conectar', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();
      expect(component.passo()).toBe('conectado');

      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_LENTO_MS);

      expect(component.passo()).toBe('reconectando');
      expect(texto()).toContain('Reconectando…');
      expect(texto()).toContain('Isso é automático, não precisa fazer nada.');
      expect(botao('Conectar com número de telefone')).toBeUndefined();
      expect(raiz().querySelector('button')).toBeNull();
      encerrar();
    }));

    it('Reconectando ao abrir a tela com INITIALIZING vindo do servidor', fakeAsync(() => {
      statusAtual = dto('INITIALIZING');
      abrir();

      expect(component.passo()).toBe('reconectando');
      encerrar();
    }));

    it('Conectando depois do código: INITIALIZING no passo do código mostra Conectando e não Reconectando', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_MS);

      expect(component.passo()).toBe('conectando');
      expect(texto()).toContain('Conectando…');
      expect(texto()).toContain('Confirmando com o WhatsApp.');
      expect(texto()).not.toContain('Reconectando…');
      encerrar();
    }));

    it('Conectando depois do QR Code: INITIALIZING no passo do QR Code mostra Conectando', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });
      clicar('Prefiro ler o QR Code');

      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_MS);

      expect(component.passo()).toBe('conectando');
      encerrar();
    }));

    it('Conectando: o QRCODE passageiro do reinício do Baileys não derruba a tela dentro da guarda', fakeAsync(() => {
      abrir();
      irParaCodigo();
      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_MS);

      statusAtual = dto('QRCODE');
      detectar(INTERVALO_STATUS_MS);
      expect(component.passo()).toBe('conectando');

      detectar(40_000);
      expect(component.passo()).toBe('conectando');
      detectar(5_000);
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('Volta sozinha: o status volta a CONNECTED e a tela mostra WhatsApp online', fakeAsync(() => {
      statusAtual = dto('INITIALIZING');
      abrir();

      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);

      expect(texto()).toContain('WhatsApp online');
      expect(component.passo()).toBe('conectado');
      encerrar();
    }));

    it('depois de conectado o polling continua em ritmo lento para perceber a queda', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();
      const antes = apiSpy.consultarStatus.calls.count();

      detectar(3000);
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes);
      detectar(2000);
      expect(apiSpy.consultarStatus.calls.count()).toBe(antes + 1);
      encerrar();
    }));

    it('Novo pareamento necessário: DISCONNECTED com sessaoEncerrada mostra a conexão encerrada e o botão de conectar', fakeAsync(() => {
      statusAtual = dto('DISCONNECTED', { sessaoEncerrada: true });
      abrir();

      expect(component.passo()).toBe('repareamento');
      expect(texto()).toContain('A conexão foi encerrada. Conecte de novo.');
      expect(texto()).toContain('Conexão encerrada');
      expect(botao('Conectar com número de telefone')).toBeDefined();

      clicar('Conectar com número de telefone');
      expect(component.passo()).toBe('telefone');
      encerrar();
    }));

    it('Novo pareamento necessário: repareamento oferece o QR Code', fakeAsync(() => {
      statusAtual = dto('DISCONNECTED', { sessaoEncerrada: true });
      abrir();
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });

      clicar('Prefiro ler o QR Code');

      expect(component.passo()).toBe('qrcode');
      encerrar();
    }));

    it('Nunca pareada: sessaoEncerrada falso ou ausente mostra o início e não a mensagem de encerramento', fakeAsync(() => {
      statusAtual = dto('DISCONNECTED', { sessaoEncerrada: false });
      abrir();
      expect(component.passo()).toBe('inicio');
      expect(texto()).not.toContain('A conexão foi encerrada');
      encerrar();

      statusAtual = dto('DISCONNECTED');
      abrir();
      expect(component.passo()).toBe('inicio');
      expect(texto()).not.toContain('A conexão foi encerrada');
      encerrar();
    }));

    it('Sem piscar durante o pareamento: DISCONNECTED logo após pedir o código mantém o passo do código por 45 s', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('DISCONNECTED');
      detectar(44_900);
      expect(component.passo()).toBe('codigo');

      detectar(1_600);
      expect(component.passo()).toBe('codigo');
      expect(component.codigo()).toBe(CODIGO_FALSO);
      encerrar();
    }));

    it('digitando o telefone, o DISCONNECTED do servidor não tira a pessoa do passo', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');
      digitar('+55 (11) 9');

      detectar(90_000);

      expect(component.passo()).toBe('telefone');
      expect((raiz().querySelector('input') as HTMLInputElement).value).toBe('+55 (11) 9');
      encerrar();
    }));

    it('DISCONNECTED com sessaoEncerrada depois da guarda leva ao repareamento', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('DISCONNECTED', { sessaoEncerrada: true });
      detectar((VALIDADE_CODIGO_SEGUNDOS + 5) * 1000);

      expect(component.passo()).toBe('repareamento');
      encerrar();
    }));

    it('INITIALIZING no passo do telefone mantém o passo e o que a pessoa digitou', fakeAsync(() => {
      abrir();
      clicar('Conectar com número de telefone');
      digitar('+55 (11) 9');

      statusAtual = dto('INITIALIZING');
      detectar(3000);

      expect(component.passo()).toBe('telefone');
      expect((raiz().querySelector('input') as HTMLInputElement).value).toBe('+55 (11) 9');
      encerrar();
    }));

    it('QRCODE vindo de conectado ou de reconectando leva ao início', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();
      expect(component.passo()).toBe('conectado');
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });
      detectar(5000);
      expect(component.passo()).toBe('inicio');
      encerrar();

      statusAtual = dto('INITIALIZING');
      abrir();
      expect(component.passo()).toBe('reconectando');
      statusAtual = dto('QRCODE');
      detectar(1500);
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('DISCONNECTED no passo do QR Code fica dentro da guarda e vai ao início fora dela', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE');
      clicar('Prefiro ler o QR Code');

      statusAtual = dto('DISCONNECTED');
      detectar(44_900);
      expect(component.passo()).toBe('qrcode');
      detectar(1_600);
      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('DISCONNECTED no passo conectando fica dentro da guarda', fakeAsync(() => {
      abrir();
      irParaCodigo();
      statusAtual = dto('INITIALIZING');
      detectar(1500);
      expect(component.passo()).toBe('conectando');

      statusAtual = dto('DISCONNECTED');
      detectar(3000);

      expect(component.passo()).toBe('conectando');
      encerrar();
    }));

    it('o QR sai quando o status deixa de ser QRCODE e o QR anterior fica quando o QRCODE vem sem imagem', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE', { qrcodeBase64: 'imagem_qr' });
      clicar('Prefiro ler o QR Code');
      detectar(1500);
      expect(component.qrcode()).toBe('imagem_qr');

      statusAtual = dto('QRCODE');
      detectar(1500);
      expect(component.qrcode()).toBe('imagem_qr');

      statusAtual = dto('DISCONNECTED');
      detectar(1500);
      expect(component.qrcode()).toBeNull();
      expect(component.passo()).toBe('qrcode');
      encerrar();
    }));

    it('o QR do servidor aparece quando a pessoa já está no passo do QR Code', fakeAsync(() => {
      abrir();
      statusAtual = dto('QRCODE');
      clicar('Prefiro ler o QR Code');
      statusAtual = dto('QRCODE', { qrcodeBase64: 'novo_qr' });
      detectar(INTERVALO_STATUS_MS);

      expect(component.srcQrcode()).toBe('data:image/png;base64,novo_qr');
      expect(component.status()).toBe('QRCODE');
      encerrar();
    }));
  });

  describe('regressões da revisão de código', () => {
    it('regressao: resposta tardia do código não sobrescreve o passo do QR Code', fakeAsync(() => {
      abrir();
      irParaCodigo();
      const resposta = new Subject<CodigoPareamentoResponseDTO>();
      apiSpy.solicitarCodigoPareamento.and.returnValue(resposta);
      clicar('Gerar outro código');

      clicar('Prefiro ler o QR Code');
      expect(component.passo()).toBe('qrcode');
      resposta.next({ sessao: SESSAO, codigo: CODIGO_FALSO });
      detectar();

      expect(component.passo()).toBe('qrcode');
      expect(component.codigo()).toBe('');
      encerrar();
    }));

    it('regressao: resposta tardia do código não sobrescreve conectado', fakeAsync(() => {
      const resposta = new Subject<CodigoPareamentoResponseDTO>();
      apiSpy.solicitarCodigoPareamento.and.returnValue(resposta);
      abrir();
      irParaCodigo();

      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);
      expect(component.passo()).toBe('conectado');
      resposta.next({ sessao: SESSAO, codigo: CODIGO_FALSO });
      detectar();

      expect(component.passo()).toBe('conectado');
      expect(component.codigo()).toBe('');
      encerrar();
    }));

    it('regressao: DISCONNECTED passageiro depois de 45 s não tira a pessoa do código ainda válido', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('DISCONNECTED');
      detectar(60_000);

      expect(component.passo()).toBe('codigo');
      expect(component.codigo()).toBe(CODIGO_FALSO);
      encerrar();
    }));

    it('regressao: DISCONNECTED não apaga o passo de código expirado', fakeAsync(() => {
      abrir();
      irParaCodigo();

      statusAtual = dto('DISCONNECTED');
      detectar((VALIDADE_CODIGO_SEGUNDOS + 60) * 1000);

      expect(component.passo()).toBe('codigo-expirado');
      encerrar();
    }));

    it('regressao: INITIALIZING renova a guarda do passo conectando', fakeAsync(() => {
      abrir();
      irParaCodigo();
      statusAtual = dto('INITIALIZING');
      detectar(INTERVALO_STATUS_MS);
      expect(component.passo()).toBe('conectando');

      detectar(30_000);
      statusAtual = dto('DISCONNECTED');
      detectar(30_000);

      expect(component.passo()).toBe('conectando');
      encerrar();
    }));

    it('regressao: gateway fora do ar depois de conectado mostra Reconectando e não os botões de conectar', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();
      expect(component.passo()).toBe('conectado');

      statusAtual = dto('DISCONNECTED');
      detectar(INTERVALO_STATUS_LENTO_MS);

      expect(component.passo()).toBe('reconectando');
      expect(botao('Conectar com número de telefone')).toBeUndefined();

      statusAtual = dto('CONNECTED');
      detectar(INTERVALO_STATUS_MS);
      expect(component.passo()).toBe('conectado');
      encerrar();
    }));

    it('regressao: sessaoEncerrada depois de conectado vai direto ao repareamento, sem esperar a guarda', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();

      statusAtual = dto('DISCONNECTED', { sessaoEncerrada: true });
      detectar(INTERVALO_STATUS_LENTO_MS);

      expect(component.passo()).toBe('repareamento');
      encerrar();
    }));

    it('regressao: DISCONNECTED por mais que a guarda depois de conectado leva ao início', fakeAsync(() => {
      statusAtual = dto('CONNECTED');
      abrir();

      statusAtual = dto('DISCONNECTED');
      detectar(GUARDA_DESCONECTADO_MS + 2 * INTERVALO_STATUS_LENTO_MS);

      expect(component.passo()).toBe('inicio');
      encerrar();
    }));

    it('regressao: depois de 403 ao consultar, iniciar uma conexão retoma o polling', fakeAsync(() => {
      apiSpy.consultarStatus.and.returnValue(throwError(() => erroHttp(403)));
      abrir();
      detectar(6000);
      expect(apiSpy.consultarStatus).toHaveBeenCalledTimes(1);

      apiSpy.consultarStatus.and.callFake(() => of(dto('QRCODE', { qrcodeBase64: 'qr' })));
      clicar('Prefiro ler o QR Code');
      detectar(INTERVALO_STATUS_MS * 2);

      expect(apiSpy.consultarStatus.calls.count()).toBeGreaterThan(2);
      encerrar();
    }));
  });
});
