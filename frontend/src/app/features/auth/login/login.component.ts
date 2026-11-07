import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { ChatFlutuanteComponent } from '../../../shared/components/chat-flutuante/chat-flutuante.component';
import contentData from '../../../../../../especificacao/front/login.content.json';
import designData from '../../../../../../especificacao/front/telas.design.json';

const PADRAO_EMAIL = /^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)+$/i;

function emailValido(controle: AbstractControl): ValidationErrors | null {
  const valor = String(controle.value ?? '').trim();
  if (!valor) {
    return { required: true };
  }
  return PADRAO_EMAIL.test(valor) ? null : { email: true };
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, ChatFlutuanteComponent],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly conteudo = contentData.login;
  readonly design = designData.telas.login;
  readonly mostrarSenha = signal(false);

  erro = '';
  enviando = false;

  readonly form = this.fb.nonNullable.group({
    email: ['', [emailValido]],
    senha: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor() {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl(this.auth.rotaInicial());
    }
  }

  get emailInvalido(): boolean {
    return this.form.controls.email.touched && this.form.controls.email.invalid;
  }

  alternarSenha(): void {
    this.mostrarSenha.update((visivel) => !visivel);
  }

  entrar(): void {
    this.erro = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const bruto = this.form.getRawValue();
    this.enviando = true;
    this.auth.login({ email: bruto.email.trim(), senha: bruto.senha }).subscribe({
      next: () => {
        this.enviando = false;
        void this.router.navigateByUrl(this.auth.rotaInicial());
      },
      error: (erro: unknown) => {
        this.enviando = false;
        this.erro = erro instanceof HttpErrorResponse && erro.status === 401
          ? this.conteudo.erros.credenciaisInvalidas
          : mensagemHttp(erro);
      }
    });
  }
}
