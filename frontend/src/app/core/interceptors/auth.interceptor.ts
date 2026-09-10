import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  let headers = req.headers;

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
    return erro.error?.message ?? 'Não autorizado. Faça login novamente.';
  }

  if (erro.status === 404) {
    return erro.error?.message ?? 'Recurso não encontrado.';
  }

  if (erro.status === 405) {
    return 'A API rejeitou o método HTTP. Conectar WhatsApp usa POST em /whatsapp/{sessao}/iniciar.';
  }

  if (erro.status === 400) {
    return erro.error?.message ?? 'Sessão WhatsApp já ocupada ou dados inválidos.';
  }

  return erro.error?.message ?? `Erro ${erro.status} ao falar com a API.`;
}
