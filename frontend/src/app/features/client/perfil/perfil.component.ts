import { Component, NgZone, OnInit, inject, signal } from '@angular/core';
import { TextFieldModule } from '@angular/cdk/text-field';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';

declare var cloudinary: any;
import { BannerAvisosComponent } from '../../../shared/components/banner-avisos/banner-avisos.component';
import { ConexaoWhatsappComponent } from '../../../shared/components/conexao-whatsapp/conexao-whatsapp.component';
import { AvisoDTO } from '../../../shared/models/aviso.models';
import { EmpresaView, MAX_URLS_GALERIA, MODULOS_MENU_BOT, ModuloMenuDTO } from '../../../shared/models/empresa.models';

type AbaPerfil = 'dados' | 'robo' | 'configuracao';

@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, TextFieldModule, BannerAvisosComponent, ConexaoWhatsappComponent],
  templateUrl: './perfil.component.html'
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly empresasApi = inject(EmpresaService);
  private readonly avisosApi = inject(AvisoService);
  private readonly zone = inject(NgZone);

  readonly catalogoMenu = MODULOS_MENU_BOT;
  readonly maxUrlsGaleria = MAX_URLS_GALERIA;
  readonly limiteCaracteres = 1000;
  readonly avisos = signal<AvisoDTO[]>([]);
  readonly salvando = signal(false);
  readonly salvandoSenha = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly aba = signal<AbaPerfil>('dados');
  readonly imagemSelecionada = signal('');
  readonly camposTruncados = signal<Record<string, boolean>>({});
  readonly sessao = this.auth.sessao()?.sessaoWhatsapp ?? '';
  readonly emailAcesso = this.auth.sessao()?.sub ?? '';

  readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    mensagemSaudacao: ['', Validators.maxLength(1000)],
    tabelaDePrecos: ['', Validators.maxLength(1000)],
    linkGoogleMaps: [''],
    linkFotoPrincipal: [''],
    urlsGaleria: this.fb.nonNullable.array([this.fb.nonNullable.control('')]),
    linkGaleria: [''],
    permiteReservaAutomatica: [false],
    locacaoPorHora: [false],
    regrasLocacao: ['', Validators.maxLength(1000)],
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

  get galeriaCheia(): boolean {
    return this.urlsValidas(this.urlsGaleria.getRawValue()).length >= this.maxUrlsGaleria;
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

  aplicarFormatacao(campo: string, prefixo: string, sufixo: string): void {
    const controle = this.form.get(campo);
    const elemento = document.getElementById(campo) as HTMLTextAreaElement | null;
    if (!controle || !elemento) {
      return;
    }

    const valor = String(controle.value ?? '');
    const inicio = elemento.selectionStart ?? valor.length;
    const fim = elemento.selectionEnd ?? valor.length;
    const selecionado = valor.slice(inicio, fim);
    const montado = valor.slice(0, inicio) + prefixo + selecionado + sufixo + valor.slice(fim);
    const limitado = montado.slice(0, this.limiteCaracteres);
    this.marcarTruncado(campo, montado.length > this.limiteCaracteres);
    controle.setValue(limitado);

    const cursor = Math.min(inicio + prefixo.length + selecionado.length + sufixo.length, limitado.length);
    queueMicrotask(() => {
      elemento.focus();
      elemento.setSelectionRange(Math.min(inicio + prefixo.length, cursor), cursor);
    });
  }

  aoColarTexto(evento: ClipboardEvent, campo: string): void {
    const colado = evento.clipboardData?.getData('text') ?? '';
    const elemento = evento.target as HTMLTextAreaElement;
    const atual = String(this.form.get(campo)?.value ?? '');
    const inicio = elemento.selectionStart ?? atual.length;
    const fim = elemento.selectionEnd ?? atual.length;
    const resultante = atual.slice(0, inicio) + colado + atual.slice(fim);
    this.marcarTruncado(campo, resultante.length > this.limiteCaracteres);
  }

  abrirWidgetCloudinary(tipo: 'principal' | 'galeria'): void {
    if (typeof cloudinary === 'undefined') {
      this.erro.set('O widget do Cloudinary não carregou. Recarregue a página.');
      return;
    }

    const principal = tipo === 'principal';
    const widget = cloudinary.createUploadWidget(
      {
        cloudName: environment.cloudinary.cloudName,
        uploadPreset: environment.cloudinary.uploadPreset,
        resourceType: 'image',
        clientAllowedFormats: ['jpg', 'jpeg', 'png', 'webp', 'gif'],
        multiple: !principal,
        maxFiles: principal ? 1 : this.maxUrlsGaleria
      },
      (_error: unknown, result: { event?: string; info?: { secure_url?: string } }) => {
        if (result?.event !== 'success' || !result.info?.secure_url) {
          return;
        }

        const url = result.info.secure_url;
        this.zone.run(() => {
          if (principal) {
            this.form.patchValue({ linkFotoPrincipal: url });
            this.imagemSelecionada.set(url);
            return;
          }
          this.adicionarUrlNaGaleria(url);
        });
      }
    );

    widget.open();
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
      mensagemSaudacao: this.limitarTexto('mensagemSaudacao', empresa.mensagemSaudacao ?? ''),
      tabelaDePrecos: this.limitarTexto('tabelaDePrecos', empresa.tabelaDePrecos ?? ''),
      linkGoogleMaps: empresa.linkGoogleMaps ?? '',
      linkFotoPrincipal: empresa.linkFotoPrincipal ?? '',
      linkGaleria: empresa.linkGaleria ?? '',
      permiteReservaAutomatica: empresa.permiteReservaAutomatica ?? false,
      locacaoPorHora: empresa.locacaoPorHora ?? false,
      regrasLocacao: this.limitarTexto('regrasLocacao', empresa.regrasLocacao ?? '')
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
    for (const url of validas) {
      this.urlsGaleria.push(this.fb.nonNullable.control(url));
    }
  }

  private adicionarUrlNaGaleria(url: string): void {
    const indiceVazio = this.urlsGaleria.controls.findIndex((controle) => !String(controle.value ?? '').trim());
    if (indiceVazio >= 0) {
      this.urlsGaleria.at(indiceVazio).setValue(url);
      this.imagemSelecionada.set(url);
      return;
    }
    if (this.urlsGaleria.length >= this.maxUrlsGaleria) {
      return;
    }
    this.urlsGaleria.push(this.fb.nonNullable.control(url));
    this.imagemSelecionada.set(url);
  }

  private limitarTexto(campo: string, valor: string): string {
    if (valor.length <= this.limiteCaracteres) {
      return valor;
    }
    this.marcarTruncado(campo, true);
    return valor.slice(0, this.limiteCaracteres);
  }

  private marcarTruncado(campo: string, truncado: boolean): void {
    this.camposTruncados.update((atual) => ({ ...atual, [campo]: truncado }));
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
