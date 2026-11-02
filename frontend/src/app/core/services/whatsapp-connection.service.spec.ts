import { provideHttpClient } from '@angular/common/http';
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
});
