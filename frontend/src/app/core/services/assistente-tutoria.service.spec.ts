import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AssistenteTutoriaService } from './assistente-tutoria.service';
import { environment } from '../../../environments/environment';
import {
  AssistenteChatRequestDTO,
  AssistenteChatResponseDTO,
  DiagnosticoResponseDTO
} from '../../shared/models/assistente.models';

describe('AssistenteTutoriaService', () => {
  let service: AssistenteTutoriaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AssistenteTutoriaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('executarDiagnostico faz requisicao GET para endpoint de diagnostico', () => {
    const mockResp: DiagnosticoResponseDTO = {
      statusGeral: 'OPERACIONAL',
      dataHora: '2026-11-03T14:30:00Z',
      modulos: [
        { modulo: 'Auth', status: 'OPERACIONAL', mensagem: 'OK', tempoRespostaMs: 10 }
      ]
    };

    service.executarDiagnostico().subscribe((res) => {
      expect(res.statusGeral).toBe('OPERACIONAL');
      expect(res.modulos.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/assistente-tutoria/diagnostico`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResp);
  });

  it('conversar faz requisicao POST com payload para endpoint de chat admin', () => {
    const payload: AssistenteChatRequestDTO = {
      mensagem: 'Como funciona?',
      modo: 'DIAGNOSTICO'
    };

    const mockResp: AssistenteChatResponseDTO = {
      resposta: 'Resposta teste da IA',
      modo: 'DIAGNOSTICO',
      dataHora: '2026-11-03T14:30:05Z',
      sugestoes: ['Dica 1']
    };

    service.conversar(payload).subscribe((res) => {
      expect(res.resposta).toBe('Resposta teste da IA');
      expect(res.modo).toBe('DIAGNOSTICO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/assistente-tutoria/chat`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResp);
  });

  it('conversarCliente faz requisicao POST com payload para endpoint de chat do cliente', () => {
    const payload: AssistenteChatRequestDTO = {
      mensagem: 'Como conectar o WhatsApp?',
      modo: 'SUPORTE'
    };

    const mockResp: AssistenteChatResponseDTO = {
      resposta: 'Passo a passo do cliente',
      modo: 'SUPORTE',
      dataHora: '2026-11-03T14:30:05Z',
      sugestoes: ['Sugestao']
    };

    service.conversarCliente(payload).subscribe((res) => {
      expect(res.resposta).toBe('Passo a passo do cliente');
      expect(res.modo).toBe('SUPORTE');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/cliente/assistente/chat`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResp);
  });
});
