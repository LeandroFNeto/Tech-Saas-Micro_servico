import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const sessao = auth.sessao();
  let headers = req.headers;

  if (sessao?.papel === 'ADMIN') {
    headers = headers.set('x-admin-token', environment.adminApiKey);
  }

  if (sessao?.papel === 'CLIENTE' && auth.token()) {
    headers = headers.set('x-cliente-token', auth.token()!);
  }

  const token = auth.token();
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  return next(req.clone({ headers })).pipe(
    catchError((erro: HttpErrorResponse) => throwError(() => erro))
  );
};

export function mensagemHttp(erro: unknown): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return 'Não foi possível concluir a operação.';
  }

  if (erro.status === 0) {
    return 'API indisponível. Confira se o Spring Boot está em http://localhost:8080.';
  }

  if (erro.status === 401) {
    return 'Token administrativo inválido. Confira ADMIN_API_KEY no .env e environment.ts.';
  }

  if (erro.status === 404) {
    return 'Empresa não encontrada para a sessão informada.';
  }

  if (erro.status === 400) {
    return erro.error?.message ?? 'Sessão WhatsApp já ocupada ou dados inválidos.';
  }

  return erro.error?.message ?? `Erro ${erro.status} ao falar com a API.`;
}
