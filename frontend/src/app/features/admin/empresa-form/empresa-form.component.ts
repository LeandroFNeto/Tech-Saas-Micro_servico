import { Component, DestroyRef, NgZone, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { EmpresaService } from '../../../core/services/empresa.service';
import { ConexaoWhatsappComponent } from '../../../shared/components/conexao-whatsapp/conexao-whatsapp.component';
import { EmpresaView, MAX_URLS_GALERIA, MODULOS_DISPONIVEIS } from '../../../shared/models/empresa.models';

declare var cloudinary: any;

@Component({
  selector: 'app-empresa-form',
  imports: [ReactiveFormsModule, RouterLink, ConexaoWhatsappComponent],
  templateUrl: './empresa-form.component.html'
})
export class EmpresaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly empresasApi = inject(EmpresaService);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  readonly modulos = MODULOS_DISPONIVEIS;
  readonly maxUrlsGaleria = MAX_URLS_GALERIA;
  readonly modoEdicao = signal(false);
  readonly sessaoOriginal = signal('');
  readonly salvando = signal(false);
  readonly salvandoSenha = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly sucessoSenha = signal('');
  readonly imagemSelecionada = signal('');

  readonly formResetSenha = this.fb.nonNullable.group({
    novaSenha: ['', [Validators.required, Validators.minLength(6)]]
  });

  readonly form = this.fb.group({
    nome: ['', Validators.required],
    sessaoWhatsapp: ['', Validators.required],
    ramoDeAtuacao: ['locacao'],
    precoBase: this.fb.control<number | null>(null),
    locacaoPorHora: [false],
    googleCalendarId: [''],
    email: [''],
    senha: [''],
    linkFotoPrincipal: [''],
    urlsGaleria: this.fb.nonNullable.array([this.fb.nonNullable.control('')]),
    linkGaleria: [''],
    modulos: this.fb.group(
      Object.fromEntries(MODULOS_DISPONIVEIS.map((modulo) => [modulo.codigo, this.fb.control(false)]))
    )
  });

  get urlsGaleria(): FormArray {
    return this.form.controls.urlsGaleria;
  }

  get galeriaCheia(): boolean {
    return (
      this.urlsGaleria.length >= this.maxUrlsGaleria &&
      this.urlsGaleria.controls.every((controle) => String(controle.value ?? '').trim().length > 0)
    );
  }

  ngOnInit(): void {
    const sessao = this.route.snapshot.paramMap.get('sessao');
    if (!sessao || sessao === 'nova') {
      this.form.controls.email.setValidators([Validators.required, Validators.email]);
      this.form.controls.senha.setValidators([Validators.required, Validators.minLength(6)]);
      this.form.controls.email.updateValueAndValidity();
      this.form.controls.senha.updateValueAndValidity();
      this.observarSessaoParaEmail();
      return;
    }

    this.modoEdicao.set(true);
    this.sessaoOriginal.set(sessao);
    this.empresasApi.buscarPorSessao(sessao).subscribe({
      next: (empresa) => this.preencher(empresa),
      error: (err) => this.erro.set(mensagemHttp(err))
    });
  }

  salvar(): void {
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    const valor = this.form.getRawValue();
    const modulosAtivos = this.modulosSelecionados();
    const urlsGaleria = this.urlsValidas(valor.urlsGaleria);

    if (this.modoEdicao()) {
      this.empresasApi
        .atualizarPeloAdmin(this.sessaoOriginal(), {
          sessaoWhatsapp: valor.sessaoWhatsapp ?? undefined,
          locacaoPorHora: valor.locacaoPorHora ?? false,
          googleCalendarId: valor.googleCalendarId || null,
          linkFotoPrincipal: valor.linkFotoPrincipal || undefined,
          urlsGaleria,
          linkGaleria: (valor.linkGaleria ?? '').trim(),
          modulosAtivos
        })
        .subscribe({
          next: () => {
            this.sucesso.set('Infraestrutura e módulos atualizados.');
            this.salvando.set(false);
            this.sessaoOriginal.set(valor.sessaoWhatsapp ?? this.sessaoOriginal());
          },
          error: (err) => {
            this.erro.set(mensagemHttp(err));
            this.salvando.set(false);
          }
        });
      return;
    }

    this.empresasApi
      .criar({
        nome: valor.nome ?? '',
        sessaoWhatsapp: valor.sessaoWhatsapp ?? '',
        ramoDeAtuacao: valor.ramoDeAtuacao ?? undefined,
        precoBase: valor.precoBase,
        googleCalendarId: valor.googleCalendarId || null,
        email: valor.email ?? '',
        senha: valor.senha ?? '',
        linkFotoPrincipal: valor.linkFotoPrincipal || undefined,
        urlsGaleria,
        linkGaleria: (valor.linkGaleria ?? '').trim() || undefined,
        modulosIniciais: modulosAtivos
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          void this.router.navigateByUrl('/admin');
        },
        error: (err) => {
          this.erro.set(mensagemHttp(err));
          this.salvando.set(false);
        }
      });
  }

  resetarSenha(): void {
    this.erro.set('');
    this.sucessoSenha.set('');
    if (this.formResetSenha.invalid) {
      this.formResetSenha.markAllAsTouched();
      return;
    }

    this.salvandoSenha.set(true);
    this.empresasApi.resetarSenha(this.sessaoOriginal(), this.formResetSenha.controls.novaSenha.value).subscribe({
      next: () => {
        this.salvandoSenha.set(false);
        this.formResetSenha.reset();
        this.sucessoSenha.set('Senha atualizada com sucesso. Informe o cliente.');
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.salvandoSenha.set(false);
      }
    });
  }

  private preencher(empresa: EmpresaView): void {
    this.form.patchValue({
      nome: empresa.nome,
      sessaoWhatsapp: empresa.sessaoWhatsapp,
      ramoDeAtuacao: empresa.ramoDeAtuacao ?? 'locacao',
      locacaoPorHora: empresa.locacaoPorHora ?? false,
      googleCalendarId: empresa.googleCalendarId ?? '',
      linkFotoPrincipal: empresa.linkFotoPrincipal ?? '',
      linkGaleria: empresa.linkGaleria ?? ''
    });
    this.preencherGaleria(empresa.urlsGaleria);
    this.imagemSelecionada.set(empresa.linkFotoPrincipal ?? empresa.urlsGaleria?.[0] ?? '');

    const grupo = this.form.controls.modulos;
    for (const modulo of this.modulos) {
      grupo.get(modulo.codigo)?.setValue(empresa.modulosAtivos.includes(modulo.codigo));
    }
  }

  private observarSessaoParaEmail(): void {
    this.atualizarEmailPelaSessao(this.form.controls.sessaoWhatsapp.value);
    this.form.controls.sessaoWhatsapp.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sessao) => this.atualizarEmailPelaSessao(sessao));
  }

  private atualizarEmailPelaSessao(sessao: string | null): void {
    const formatada = (sessao ?? '').toLowerCase().replace(/\s+/g, '');
    this.form.controls.email.setValue(formatada ? `${formatada}@gamb.com.br` : '', { emitEvent: false });
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

  private preencherGaleria(urls: string[] | undefined): void {
    this.urlsGaleria.clear();
    const validas = this.urlsValidas(urls ?? []);
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

  private modulosSelecionados(): string[] {
    const valores = this.form.controls.modulos.getRawValue();
    return Object.entries(valores)
      .filter(([, ativo]) => ativo)
      .map(([codigo]) => codigo);
  }
}
