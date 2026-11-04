import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ConexaoWhatsappComponent } from './conexao-whatsapp.component';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { SessaoStatusResponseDTO } from '../../models/whatsapp.models';

describe('ConexaoWhatsappComponent', () => {
  let component: ConexaoWhatsappComponent;
  let fixture: ComponentFixture<ConexaoWhatsappComponent>;
  let apiSpy: jasmine.SpyObj<WhatsappConnectionService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj<WhatsappConnectionService>('WhatsappConnectionService', [
      'consultarStatus',
      'iniciar'
    ]);

    apiSpy.consultarStatus.and.returnValue(of({
      sessao: 'sessao_padrao',
      status: 'DISCONNECTED',
      qrcodeBase64: null
    }));

    TestBed.configureTestingModule({
      imports: [ConexaoWhatsappComponent],
      providers: [
        { provide: WhatsappConnectionService, useValue: apiSpy }
      ]
    });

    fixture = TestBed.createComponent(ConexaoWhatsappComponent);
    component = fixture.componentInstance;
  });

  it('formata adequadamente srcQrcode com prefixo data uri se necessario', () => {
    component.qrcode.set(null);
    expect(component.srcQrcode()).toBeNull();

    component.qrcode.set('data:image/png;base64,abc');
    expect(component.srcQrcode()).toBe('data:image/png;base64,abc');

    component.qrcode.set('rawbase64string');
    expect(component.srcQrcode()).toBe('data:image/png;base64,rawbase64string');
  });

  it('iniciarConexao chama servico e atualiza estado de qrcode e status', () => {
    component.sessao = 'sessao_123';
    const mockDTO: SessaoStatusResponseDTO = {
      sessao: 'sessao_123',
      status: 'QRCODE',
      qrcodeBase64: 'imagem_qr'
    };
    apiSpy.iniciar.and.returnValue(of(mockDTO));

    component.iniciarConexao();

    expect(apiSpy.iniciar).toHaveBeenCalledWith('sessao_123');
    expect(component.status()).toBe('QRCODE');
    expect(component.qrcode()).toBe('imagem_qr');
    expect(component.iniciando()).toBeFalse();
  });

  it('iniciarConexao trata erro da API e exibe mensagem amigavel', () => {
    component.sessao = 'sessao_123';
    const httpErro = new HttpErrorResponse({ status: 400, error: { message: 'Sessao ocupada' } });
    apiSpy.iniciar.and.returnValue(throwError(() => httpErro));

    component.iniciarConexao();

    expect(component.erro()).toBe('Sessao ocupada');
    expect(component.iniciando()).toBeFalse();
  });

  it('reage a mudancas no input sessao via ngOnChanges', fakeAsync(() => {
    component.sessao = 'nova_sessao';
    apiSpy.consultarStatus.and.returnValue(of({
      sessao: 'nova_sessao',
      status: 'CONNECTED',
      qrcodeBase64: null
    }));

    component.ngOnChanges({
      sessao: new SimpleChange(null, 'nova_sessao', true)
    });
    tick();

    expect(apiSpy.consultarStatus).toHaveBeenCalledWith('nova_sessao');
    expect(component.status()).toBe('CONNECTED');
  }));
});
