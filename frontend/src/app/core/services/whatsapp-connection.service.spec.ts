import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { WhatsappConnectionService } from './whatsapp-connection.service';
import { environment } from '../../../environments/environment';
import { SessaoStatusResponseDTO } from '../../shared/models/whatsapp.models';

describe('WhatsappConnectionService', () => {
  let service: WhatsappConnectionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(WhatsappConnectionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('consultarStatus faz requisicao GET com encode da sessao', () => {
    const mockResponse: SessaoStatusResponseDTO = {
      sessao: 'sessao teste/01',
      status: 'CONNECTED',
      qrcodeBase64: null
    };

    service.consultarStatus('sessao teste/01').subscribe((res) => {
      expect(res.status).toBe('CONNECTED');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/whatsapp/sessao%20teste%2F01/status`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('iniciar faz requisicao POST com corpo vazio', () => {
    const mockResponse: SessaoStatusResponseDTO = {
      sessao: 'minha_sessao',
      status: 'QRCODE',
      qrcodeBase64: 'dados_qrcode'
    };

    service.iniciar('minha_sessao').subscribe((res) => {
      expect(res.status).toBe('QRCODE');
      expect(res.qrcodeBase64).toBe('dados_qrcode');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/whatsapp/minha_sessao/iniciar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockResponse);
  });

  it('solicitarCodigoPareamento faz POST com o telefone só no corpo e a sessao codificada na URL', () => {
    service.solicitarCodigoPareamento('sessao teste/01', '5511900000000').subscribe((res) => {
      expect(res.codigo).toBe('ABCD-EFGH');
      expect(res.sessao).toBe('sessao teste/01');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/whatsapp/sessao%20teste%2F01/codigo-pareamento`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ telefone: '5511900000000' });
    expect(req.request.url).not.toContain('5511900000000');
    expect(req.request.urlWithParams).not.toContain('5511900000000');
    req.flush({ sessao: 'sessao teste/01', codigo: 'ABCD-EFGH' });
  });

  it('solicitarCodigoPareamento nao grava o telefone nem escreve em log', () => {
    const log = spyOn(console, 'log');
    const erroLog = spyOn(console, 'error');
    const gravar = spyOn(Storage.prototype, 'setItem');

    service.solicitarCodigoPareamento('minha_sessao', '5511900000000').subscribe();
    httpMock
      .expectOne(`${environment.apiUrl}/whatsapp/minha_sessao/codigo-pareamento`)
      .flush({ sessao: 'minha_sessao', codigo: 'ABCD-EFGH' });

    expect(log).not.toHaveBeenCalled();
    expect(erroLog).not.toHaveBeenCalled();
    expect(gravar).not.toHaveBeenCalled();
  });

  [400, 401, 403, 404, 409, 429, 503].forEach((status) => {
    it(`solicitarCodigoPareamento repassa o erro ${status} sem alterar o status`, () => {
      let recebido: HttpErrorResponse | undefined;
      service.solicitarCodigoPareamento('minha_sessao', '5511900000000').subscribe({
        error: (err: HttpErrorResponse) => (recebido = err)
      });

      httpMock
        .expectOne(`${environment.apiUrl}/whatsapp/minha_sessao/codigo-pareamento`)
        .flush({ message: 'erro' }, { status, statusText: 'erro' });

      expect(recebido?.status).toBe(status);
    });
  });

  it('consultarStatus repassa sessaoEncerrada e o status INITIALIZING', () => {
    const mockResponse: SessaoStatusResponseDTO = {
      sessao: 'minha_sessao',
      status: 'DISCONNECTED',
      qrcodeBase64: null,
      sessaoEncerrada: true
    };

    service.consultarStatus('minha_sessao').subscribe((res) => {
      expect(res.sessaoEncerrada).toBeTrue();
    });
    httpMock.expectOne(`${environment.apiUrl}/whatsapp/minha_sessao/status`).flush(mockResponse);

    service.consultarStatus('minha_sessao').subscribe((res) => {
      expect(res.status).toBe('INITIALIZING');
      expect(res.sessaoEncerrada).toBeUndefined();
    });
    httpMock
      .expectOne(`${environment.apiUrl}/whatsapp/minha_sessao/status`)
      .flush({ sessao: 'minha_sessao', status: 'INITIALIZING', qrcodeBase64: null });
  });
});
