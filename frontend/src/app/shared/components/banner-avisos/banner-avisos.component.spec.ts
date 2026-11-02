import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BannerAvisosComponent } from './banner-avisos.component';
import { AvisoDTO } from '../../models/aviso.models';

describe('BannerAvisosComponent', () => {
  let component: BannerAvisosComponent;
  let fixture: ComponentFixture<BannerAvisosComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BannerAvisosComponent]
    });

    fixture = TestBed.createComponent(BannerAvisosComponent);
    component = fixture.componentInstance;
  });

  it('separa avisos de manutencao dos demais avisos', () => {
    const lista: AvisoDTO[] = [
      { id: 1, tipo: 'MANUTENCAO', titulo: 'Manutenção 1', conteudo: 'Aviso', ativo: true },
      { id: 2, tipo: 'TUTORIAL', titulo: 'Tutorial 1', conteudo: 'Guia', ativo: true },
      { id: 3, tipo: 'PRECO', titulo: 'Novo Plano', conteudo: 'Valores', ativo: true }
    ];

    component.avisos = lista;

    expect(component.manutencoes().length).toBe(1);
    expect(component.manutencoes()[0].id).toBe(1);

    expect(component.demais().length).toBe(2);
  });

  it('retorna CTA e estilos visuais correspondentes ao tipo de aviso', () => {
    expect(component.cta('TUTORIAL')).toBe('Ver tutorial');
    expect(component.cta('PRECO')).toBe('Ver preços');
    expect(component.cta('COMUNICADO')).toBe('Saiba mais');

    expect(component.cardTipo('PRECO')).toContain('bg-accent-50');
    expect(component.cardTipo('MANUTENCAO')).toContain('bg-red-50');
    expect(component.cardTipo('TUTORIAL')).toContain('bg-white');

    expect(component.textoTipo('TUTORIAL')).toBe('text-brand-700');
    expect(component.textoTipo('PRECO')).toBe('text-accent-800');
    expect(component.textoTipo('MANUTENCAO')).toBe('text-red-700');
    expect(component.textoTipo('COMUNICADO')).toBe('text-brand-600');
  });

  it('exibe mensagem quando compacto e sem outros avisos', () => {
    component.avisos = [];
    component.compacto = true;
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Nenhum aviso ativo no momento.');
  });
});
