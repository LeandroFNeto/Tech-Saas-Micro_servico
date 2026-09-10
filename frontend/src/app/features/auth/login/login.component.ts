import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';

const EMAIL_GAMB = /^[a-zA-Z0-9._%+-]+@gamb\.com\.br$/;

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  erro = '';
  enviando = false;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.pattern(EMAIL_GAMB)]],
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

  entrar(): void {
    this.erro = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando = true;
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.enviando = false;
        void this.router.navigateByUrl(this.auth.rotaInicial());
      },
      error: (erro: unknown) => {
        this.enviando = false;
        this.erro = erro instanceof HttpErrorResponse && erro.status === 401
          ? 'Senha incorreta'
          : mensagemHttp(erro);
      }
    });
  }
}
