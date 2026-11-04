import { ComponentFixture, TestBed, fakeAsync, discardPeriodicTasks, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { EmpresaView } from '../../../shared/models/empresa.models';
import { SessaoStatusResponseDTO } from '../../../shared/models/whatsapp.models';
import { DashboardComponent } from './dashboard.component';

const empresa: EmpresaView = {
  nome: 'Casa de Festas',
  sessaoWhatsapp: 'casa',
  modulosAtivos: [],
  modulosMenu: []
};

const mockAuth = {
  isAdmin: () => true,
  isSuperAdmin: () => true,
  isLeitura: () => false,
  possuiPapel: () => true
};

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let status: SessaoStatusResponseDTO;

  beforeEach(() => {
    status = { sessao: 'casa', status: 'DISCONNECTED', qrcodeBase64: null };
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: EmpresaService, useValue: { listar: () => of([empresa]), excluir: () => of(undefined) } },
        { provide: WhatsappConnectionService, useValue: { consultarStatus: () => of(status) } },
        { provide: AuthService, useValue: mockAuth }
      ]
    });
    fixture = TestBed.createComponent(DashboardComponent);
  });

  it('atualiza o chip de status a cada consulta', fakeAsync(() => {
    fixture.detectChanges();
    tick(0);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Desconectado');

    status = { sessao: 'casa', status: 'CONNECTED', qrcodeBase64: null };
    tick(5000);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('WhatsApp Online');
    discardPeriodicTasks();
  }));

  it('lista só com o botão Editar, sem botão de excluir', fakeAsync(() => {
    fixture.detectChanges();
    tick(0);
    fixture.detectChanges();

    const raiz: HTMLElement = fixture.nativeElement;
    expect(raiz.querySelector('a[aria-label="Editar Casa de Festas"]')).toBeTruthy();
    expect(raiz.querySelector('button[aria-label="Excluir Casa de Festas"]')).toBeNull();
    expect(raiz.textContent).not.toContain('Excluir');
    discardPeriodicTasks();
  }));
});
