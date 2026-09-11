import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { BannerAvisosComponent } from '../../../shared/components/banner-avisos/banner-avisos.component';
import { ConexaoWhatsappComponent } from '../../../shared/components/conexao-whatsapp/conexao-whatsapp.component';
import { AvisoDTO } from '../../../shared/models/aviso.models';
import { EmpresaView, MAX_URLS_GALERIA, MODULOS_MENU_BOT, ModuloMenuDTO } from '../../../shared/models/empresa.models';

type AbaPerfil = 'dados' | 'robo' | 'configuracao';

@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, BannerAvisosComponent, ConexaoWhatsappComponent],
  templateUrl: './perfil.component.html'
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly empresasApi = inject(EmpresaService);
  private readonly avisosApi = inject(AvisoService);

  readonly catalogoMenu = MODULOS_MENU_BOT;
  readonly maxUrlsGaleria = MAX_URLS_GALERIA;
  readonly avisos = signal<AvisoDTO[]>([]);
  readonly salvando = signal(false);
  readonly salvandoSenha = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly aba = signal<AbaPerfil>('dados');
  readonly imagemSelecionada = signal('');
  readonly sessao = this.auth.sessao()?.sessaoWhatsapp ?? '';
  readonly emailAcesso = this.auth.sessao()?.sub ?? '';

  readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    mensagemSaudacao: [''],
    tabelaDePrecos: [''],
    linkGoogleMaps: [''],
    linkFotoPrincipal: [''],
    urlsGaleria: this.fb.nonNullable.array([this.fb.nonNullable.control('')]),
    linkGaleria: [''],
    permiteReservaAutomatica: [false],
    locacaoPorHora: [false],
    regrasLocacao: [''],
    modulosMenu: this.fb.array(MODULOS_MENU_BOT.map((item) => this.grupoModulo(item.codigo, item.textoPadrao, false)))
  });

  readonly formSenha = this.fb.nonNullable.group(
    {
      senhaAtual: ['', [Validators.required, Validators.minLength(6)]],
      novaSenha: ['', [Validators.required, Validators.minLength(6)]],
      confirmarSenha: ['', [Validators.required, Validators.minLength(6)]]
    },
    { validators: senhasIguais }
  );

  get urlsGaleria() {
    return this.form.controls.urlsGaleria;
  }

  get senhasNaoConferem(): boolean {
    return this.formSenha.touched && this.formSenha.hasError('senhasDiferentes');
  }

  get modulosMenu() {
    return this.form.controls.modulosMenu;
  }

  ngOnInit(): void {
    this.avisosApi.listarAtivos().subscribe({
      next: (lista) => this.avisos.set(lista),
      error: (err) => this.erro.set(mensagemHttp(err))
    });

    if (!this.sessao) {
      this.erro.set('Sessão WhatsApp não encontrada neste usuário.');
      return;
    }

    this.empresasApi.buscarPorSessao(this.sessao).subscribe({
      next: (empresa) => this.preencher(empresa),
      error: (err) => this.erro.set(mensagemHttp(err))
    });
  }

  rotuloModulo(codigo: string): string {
    return this.catalogoMenu.find((item) => item.codigo === codigo)?.label ?? codigo;
  }

  descricaoModulo(codigo: string): string {
    return this.catalogoMenu.find((item) => item.codigo === codigo)?.descricao ?? '';
  }

  salvar(): void {
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid || !this.sessao) {
      this.form.markAllAsTouched();
      this.aba.set('dados');
      return;
    }

    this.salvando.set(true);
    const bruto = this.form.getRawValue();
    this.empresasApi
      .atualizarPeloCliente(this.sessao, {
        ...bruto,
        urlsGaleria: this.urlsValidas(bruto.urlsGaleria),
        linkGaleria: bruto.linkGaleria.trim()
      })
      .subscribe({
      next: () => {
        this.sucesso.set('Perfil e configurações do robô atualizados.');
        this.salvando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.salvando.set(false);
      }
    });
  }

  alterarSenha(): void {
    this.erro.set('');
    this.sucesso.set('');
    if (this.formSenha.invalid) {
      this.formSenha.markAllAsTouched();
      return;
    }

    this.salvandoSenha.set(true);
    const valor = this.formSenha.getRawValue();
    this.auth
      .alterarSenha({ senhaAtual: valor.senhaAtual, novaSenha: valor.novaSenha })
      .subscribe({
        next: () => {
          this.formSenha.reset();
          this.salvandoSenha.set(false);
          this.sucesso.set('Senha atualizada. Use a nova senha no próximo acesso.');
        },
        error: (err) => {
          this.salvandoSenha.set(false);
          this.erro.set(
            err instanceof HttpErrorResponse && err.status === 401
              ? 'Senha atual incorreta.'
              : mensagemHttp(err)
          );
        }
      });
  }

  selecionarImagem(url: string | null | undefined): void {
    const valor = (url ?? '').trim();
    if (valor) {
      this.imagemSelecionada.set(valor);
    }
  }

  adicionarUrlGaleria(): void {
    if (this.urlsGaleria.length >= this.maxUrlsGaleria) {
      return;
    }
    this.urlsGaleria.push(this.fb.nonNullable.control(''));
  }

  removerUrlGaleria(indice: number): void {
    this.urlsGaleria.removeAt(indice);
    if (this.urlsGaleria.length === 0) {
      this.adicionarUrlGaleria();
    }
  }

  private grupoModulo(codigoAcao: string, textoMenu: string, ativo: boolean) {
    return this.fb.nonNullable.group({
      codigoAcao: [codigoAcao],
      textoMenu: [textoMenu],
      ativo: [ativo]
    });
  }

  private preencher(empresa: EmpresaView): void {
    this.form.patchValue({
      nome: empresa.nome,
      mensagemSaudacao: empresa.mensagemSaudacao ?? '',
      tabelaDePrecos: empresa.tabelaDePrecos ?? '',
      linkGoogleMaps: empresa.linkGoogleMaps ?? '',
      linkFotoPrincipal: empresa.linkFotoPrincipal ?? '',
      linkGaleria: empresa.linkGaleria ?? '',
      permiteReservaAutomatica: empresa.permiteReservaAutomatica ?? false,
      locacaoPorHora: empresa.locacaoPorHora ?? false,
      regrasLocacao: empresa.regrasLocacao ?? ''
    });
    this.preencherGaleria(empresa.urlsGaleria);
    this.imagemSelecionada.set(empresa.linkFotoPrincipal ?? empresa.urlsGaleria?.[0] ?? '');

    for (const grupo of this.modulosMenu.controls) {
      const codigo = grupo.controls.codigoAcao.value;
      const salvo = this.buscarModulo(empresa, codigo);
      const padrao = this.catalogoMenu.find((item) => item.codigo === codigo)?.textoPadrao ?? codigo;
      grupo.patchValue({
        textoMenu: salvo?.textoMenu || padrao,
        ativo: salvo?.ativo ?? false
      });
    }
  }

  private preencherGaleria(urls: string[] | undefined): void {
    this.urlsGaleria.clear();
    const validas = (urls ?? []).map((url) => url.trim()).filter((url) => url.length > 0).slice(0, this.maxUrlsGaleria);
    if (validas.length === 0) {
      this.adicionarUrlGaleria();
      return;
    }
    for (const url of validas) {
      this.urlsGaleria.push(this.fb.nonNullable.control(url));
    }
  }

  private urlsValidas(urls: string[]): string[] {
    return urls.map((url) => url.trim()).filter((url) => url.length > 0).slice(0, this.maxUrlsGaleria);
  }

  private buscarModulo(empresa: EmpresaView, codigo: string): ModuloMenuDTO | undefined {
    return empresa.modulosMenu.find((modulo) => modulo.codigoAcao === codigo);
  }
}

function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const nova = grupo.get('novaSenha')?.value;
  const confirmar = grupo.get('confirmarSenha')?.value;
  if (!nova || !confirmar || nova === confirmar) {
    return null;
  }
  return { senhasDiferentes: true };
}
