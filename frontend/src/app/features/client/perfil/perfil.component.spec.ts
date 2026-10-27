import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { WhatsappConnectionService } from '../../../core/services/whatsapp-connection.service';
import { EmpresaView } from '../../../shared/models/empresa.models';
import { PerfilComponent } from './perfil.component';

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

function montar(empresa: EmpresaView): ComponentFixture<PerfilComponent> {
  TestBed.configureTestingModule({
    imports: [PerfilComponent],
    providers: [
      { provide: AuthService, useValue: { sessao: () => ({ sessaoWhatsapp: 'casa', sub: 'cliente@casa.com' }) } },
      { provide: AvisoService, useValue: { listarAtivos: () => of([]) } },
      { provide: EmpresaService, useValue: { buscarPorSessao: () => of(empresa) } },
      {
        provide: WhatsappConnectionService,
        useValue: { consultarStatus: () => of({ sessao: 'casa', status: 'DISCONNECTED', qrcodeBase64: null }) }
      }
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

describe('PerfilComponent, fotos pelo Cloudinary', () => {
  it('abre a foto principal e cada foto da galeria em nova aba', () => {
    const fixture = montar(empresaCom(true));
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a[target="_blank"]'));
    expect(links.map((a) => a.getAttribute('href'))).toEqual([FOTO, ...GALERIA]);
    expect(links.every((a) => a.getAttribute('rel')?.includes('noopener'))).toBeTrue();
  });

  it('não mostra link quando ainda não há foto', () => {
    const fixture = montar(empresaCom(false));
    expect(fixture.nativeElement.querySelectorAll('a[target="_blank"]').length).toBe(0);
  });

  it('os botões de envio abrem o widget da foto principal e da galeria', () => {
    const fixture = montar(empresaCom(false));
    const abrir = spyOn(fixture.componentInstance, 'abrirWidgetCloudinary');
    botao(fixture, 'Enviar foto principal').click();
    botao(fixture, 'Enviar fotos').click();
    expect(abrir.calls.allArgs()).toEqual([['principal'], ['galeria']]);
  });

  it('avisa quando o script do Cloudinary não carregou', () => {
    const fixture = montar(empresaCom(false));
    botao(fixture, 'Enviar foto principal').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('O widget do Cloudinary não carregou');
  });
});
