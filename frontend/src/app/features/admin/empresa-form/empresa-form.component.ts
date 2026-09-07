import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { EmpresaService } from '../../../core/services/empresa.service';
import { EmpresaView, MODULOS_DISPONIVEIS } from '../../../shared/models/empresa.models';

@Component({
  selector: 'app-empresa-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './empresa-form.component.html'
})
export class EmpresaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly empresasApi = inject(EmpresaService);

  readonly modulos = MODULOS_DISPONIVEIS;
  readonly modoEdicao = signal(false);
  readonly sessaoOriginal = signal('');
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');

  readonly form = this.fb.group({
    nome: ['', Validators.required],
    sessaoWhatsapp: ['', Validators.required],
    ramoDeAtuacao: ['locacao'],
    precoBase: this.fb.control<number | null>(null),
    locacaoPorHora: [false],
    modulos: this.fb.group(
      Object.fromEntries(MODULOS_DISPONIVEIS.map((modulo) => [modulo.codigo, this.fb.control(false)]))
    )
  });

  ngOnInit(): void {
    const sessao = this.route.snapshot.paramMap.get('sessao');
    if (!sessao || sessao === 'nova') {
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

    if (this.modoEdicao()) {
      this.empresasApi
        .atualizarPeloAdmin(this.sessaoOriginal(), {
          sessaoWhatsapp: valor.sessaoWhatsapp ?? undefined,
          locacaoPorHora: valor.locacaoPorHora ?? false,
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

  private preencher(empresa: EmpresaView): void {
    this.form.patchValue({
      nome: empresa.nome,
      sessaoWhatsapp: empresa.sessaoWhatsapp,
      ramoDeAtuacao: empresa.ramoDeAtuacao ?? 'locacao',
      locacaoPorHora: empresa.locacaoPorHora ?? false
    });

    const grupo = this.form.controls.modulos;
    for (const modulo of this.modulos) {
      grupo.get(modulo.codigo)?.setValue(empresa.modulosAtivos.includes(modulo.codigo));
    }
  }

  private modulosSelecionados(): string[] {
    const valores = this.form.controls.modulos.getRawValue();
    return Object.entries(valores)
      .filter(([, ativo]) => ativo)
      .map(([codigo]) => codigo);
  }
}
