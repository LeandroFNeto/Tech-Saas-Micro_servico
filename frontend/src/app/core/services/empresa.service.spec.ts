import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EmpresaService } from './empresa.service';
import { environment } from '../../../environments/environment';
import { EmpresaCreateDTO, EmpresaUpdateAdminDTO, EmpresaUpdateDTO } from '../../shared/models/empresa.models';

describe('EmpresaService', () => {
  let service: EmpresaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(EmpresaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listar mapeia colecao HAL de empresas para EmpresaView', () => {
    service.listar().subscribe((empresas) => {
      expect(empresas.length).toBe(1);
      expect(empresas[0].nome).toBe('Chácara Boa Vista');
      expect(empresas[0].modulosAtivos).toEqual(['LOCACAO', 'PREVISAO']);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas`);
    expect(req.request.method).toBe('GET');
    req.flush({
      _embedded: {
        empresas: [
          {
            id: 1,
            nome: 'Chácara Boa Vista',
            sessaoWhatsapp: 'chacara_01',
            modulosAtivos: ['LOCACAO', 'PREVISAO'],
            ativo: true
          }
        ]
      }
    });
  });

  it('buscarPorSessao busca empresa por parametro e converte para view', () => {
    service.buscarPorSessao('sessao_teste').subscribe((empresa) => {
      expect(empresa.sessaoWhatsapp).toBe('sessao_teste');
      expect(empresa.nome).toBe('Rancho Alegre');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/search/findBySessaoWhatsapp?sessaoWhatsapp=sessao_teste`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 2, nome: 'Rancho Alegre', sessaoWhatsapp: 'sessao_teste' });
  });

  it('criar envia POST com dados de criacao', () => {
    const dto: EmpresaCreateDTO = {
      nome: 'Nova Empresa',
      sessaoWhatsapp: 'nova_empresa',
      ramoDeAtuacao: 'LOCACAO',
      senha: 'SenhaForte123'
    };

    service.criar(dto).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 10, ...dto });
  });

  it('atualizarPeloAdmin envia PUT para rota admin', () => {
    const dto: EmpresaUpdateAdminDTO = { locacaoPorHora: true };

    service.atualizarPeloAdmin('sessao_1', dto).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_1/admin`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, locacaoPorHora: true });
  });

  it('atualizarPeloCliente envia PUT para rota cliente', () => {
    const dto: EmpresaUpdateDTO = { mensagemSaudacao: 'Bem-vindo!' };

    service.atualizarPeloCliente('sessao_1', dto).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_1/cliente`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, mensagemSaudacao: 'Bem-vindo!' });
  });

  it('resetarSenha envia PUT com nova senha', () => {
    service.resetarSenha('sessao_1', 'NovaSenha123').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_1/reset-senha`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ novaSenha: 'NovaSenha123' });
    req.flush(null);
  });

  it('excluir envia DELETE para sessao informada', () => {
    service.excluir('sessao_1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/empresas/sessao_1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('paraView lida com modulos como objetos ModuloEmpresa', () => {
    const view = service.paraView({
      id: 5,
      nome: 'Teste',
      sessaoWhatsapp: 'sessao_5',
      modulosAtivos: [
        { codigoAcao: 'PREVISAO', textoMenu: 'Previsão do Tempo', ordemExibicao: 1, ativo: true }
      ]
    });

    expect(view.modulosAtivos).toEqual(['PREVISAO']);
    expect(view.modulosMenu).toEqual([
      { codigoAcao: 'PREVISAO', textoMenu: 'Previsão do Tempo', ordemExibicao: 1, ativo: true }
    ]);
  });

  it('paraView mapeia lembreteConfirmacao, permitePrevisaoTempo e aceita fallback legado lembrete', () => {
    const viewNovo = service.paraView({
      id: 7,
      nome: 'Espaço Sol',
      sessaoWhatsapp: 'espaco_sol',
      permitePrevisaoTempo: false,
      lembreteConfirmacao: { modo: 'AUTOMATICO', diasAntes: 1, horarioEnvio: '09:00', texto: 'Confirme' }
    });

    expect(viewNovo.permitePrevisaoTempo).toBeFalse();
    expect(viewNovo.lembreteConfirmacao).toEqual({
      modo: 'AUTOMATICO',
      diasAntes: 1,
      horarioEnvio: '09:00',
      texto: 'Confirme'
    });

    const viewLegado = service.paraView({
      id: 8,
      nome: 'Espaço Lua',
      sessaoWhatsapp: 'espaco_lua',
      lembrete: { modo: 'SUGERIDO', diasAntes: 2, horarioEnvio: '10:00', texto: 'Olá' }
    });
    expect(viewLegado.permitePrevisaoTempo).toBeTrue();
    expect(viewLegado.lembreteConfirmacao?.modo).toBe('SUGERIDO');
  });
});
