import { TestBed } from '@angular/core/testing';
import { V0DashboardComponent } from './v0-dashboard.component';

describe('V0DashboardComponent', () => {
  it('renderiza o cabecalho e a secao de colagem do v0', () => {
    TestBed.configureTestingModule({
      imports: [V0DashboardComponent]
    });
    const fixture = TestBed.createComponent(V0DashboardComponent);
    fixture.detectChanges();
    const raiz: HTMLElement = fixture.nativeElement;

    expect(raiz.querySelector('h1')?.textContent?.trim()).toBe(fixture.componentInstance.conteudo.titulo);
    expect(raiz.querySelector('[data-v0-colar]')).not.toBeNull();
  });
});
