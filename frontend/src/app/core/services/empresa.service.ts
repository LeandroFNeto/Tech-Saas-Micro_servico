import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EmpresaCreateDTO,
  EmpresaResponseDTO,
  EmpresaUpdateAdminDTO,
  EmpresaUpdateDTO,
  EmpresaView,
  ModuloEmpresa,
  ModuloMenuDTO
} from '../../shared/models/empresa.models';

interface HalCollection {
  _embedded?: {
    empresas?: unknown[];
    empresaList?: unknown[];
  };
}

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  private readonly base = `${environment.apiUrl}/empresas`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<EmpresaView[]> {
    return this.http.get<HalCollection>(this.base).pipe(
      map((resposta) => {
        const itens = resposta._embedded?.empresas ?? resposta._embedded?.empresaList ?? [];
        return itens.map((item) => this.paraView(item));
      })
    );
  }

  buscarPorSessao(sessao: string): Observable<EmpresaView> {
    return this.http
      .get<unknown>(`${this.base}/search/findBySessaoWhatsapp`, {
        params: { sessaoWhatsapp: sessao }
      })
      .pipe(map((item) => this.paraView(item)));
  }

  criar(dto: EmpresaCreateDTO): Observable<EmpresaResponseDTO> {
    return this.http.post<EmpresaResponseDTO>(this.base, dto);
  }

  atualizarPeloAdmin(sessao: string, dto: EmpresaUpdateAdminDTO): Observable<EmpresaResponseDTO> {
    return this.http.put<EmpresaResponseDTO>(`${this.base}/${sessao}/admin`, dto);
  }

  atualizarPeloCliente(sessao: string, dto: EmpresaUpdateDTO): Observable<EmpresaResponseDTO> {
    return this.http.put<EmpresaResponseDTO>(`${this.base}/${sessao}/cliente`, dto);
  }

  paraView(bruto: unknown): EmpresaView {
    const item = (bruto ?? {}) as Record<string, unknown>;
    return {
      id: item['id'] as number | undefined,
      nome: String(item['nome'] ?? ''),
      sessaoWhatsapp: String(item['sessaoWhatsapp'] ?? ''),
      statusSessao: item['statusSessao'] as string | undefined,
      ramoDeAtuacao: item['ramoDeAtuacao'] as string | undefined,
      mensagemSaudacao: item['mensagemSaudacao'] as string | undefined,
      tabelaDePrecos: item['tabelaDePrecos'] as string | undefined,
      linkGoogleMaps: item['linkGoogleMaps'] as string | undefined,
      linkFotoPrincipal: item['linkFotoPrincipal'] as string | undefined,
      urlsGaleria: Array.isArray(item['urlsGaleria'])
        ? (item['urlsGaleria'] as unknown[]).filter((url): url is string => typeof url === 'string')
        : [],
      usaIA: Boolean(item['usaIA']),
      permiteReservaAutomatica: Boolean(item['permiteReservaAutomatica']),
      regrasLocacao: item['regrasLocacao'] as string | undefined,
      locacaoPorHora: Boolean(item['locacaoPorHora']),
      googleCalendarId: item['googleCalendarId'] as string | undefined,
      modulosAtivos: this.extrairCodigos(item['modulosAtivos']),
      modulosMenu: this.extrairModulos(item['modulosAtivos'] ?? item['modulosMenu']),
      atualizadoEm: item['atualizadoEm'] as string | undefined
    };
  }

  private extrairCodigos(modulos: unknown): string[] {
    if (!Array.isArray(modulos)) {
      return [];
    }

    return modulos
      .map((modulo) => {
        if (typeof modulo === 'string') {
          return modulo;
        }
        const entidade = modulo as ModuloEmpresa;
        return entidade.codigoAcao;
      })
      .filter((codigo): codigo is string => !!codigo);
  }

  private extrairModulos(modulos: unknown): ModuloMenuDTO[] {
    if (!Array.isArray(modulos)) {
      return [];
    }

    return modulos
      .map((modulo) => {
        if (typeof modulo === 'string') {
          return { codigoAcao: modulo, ativo: true };
        }
        const entidade = modulo as ModuloEmpresa;
        if (!entidade.codigoAcao) {
          return null;
        }
        return {
          codigoAcao: entidade.codigoAcao,
          textoMenu: entidade.textoMenu,
          ordemExibicao: entidade.ordemExibicao,
          ativo: entidade.ativo
        };
      })
      .filter((modulo): modulo is ModuloMenuDTO => modulo != null);
  }
}
