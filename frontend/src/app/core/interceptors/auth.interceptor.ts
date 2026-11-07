import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

function destinoApi(url: string): boolean {
  const api = environment.apiUrl;
  if (api && (url === api || url.startsWith(`${api}/`) || url.startsWith(`${api}?`))) {
    return true;
  }
  try {
    return new URL(url, window.location.origin).origin === window.location.origin;
  } catch {
    return false;
  }
}

function extrairMensagem(erro: HttpErrorResponse): string | null {
  const msg = erro.error?.message;
  return typeof msg === 'string' && msg.trim() ? msg.trim() : null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  let headers = req.headers;

  const token = auth.token();
  if (token && destinoApi(req.url)) {
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
    return 'Serviço temporariamente indisponível. Verifique sua conexão e tente novamente.';
  }

  const doServidor = extrairMensagem(erro);
  if (doServidor) {
    return doServidor;
  }

  switch (erro.status) {
    case 400:
      return 'Dados inválidos. Confira os campos informados.';
    case 401:
      return 'Não autorizado. Faça login novamente.';
    case 403:
      return 'Acesso negado para esta operação.';
    case 404:
      return 'Recurso não encontrado.';
    case 405:
      return 'Método HTTP não permitido para este recurso.';
    case 409:
      return 'Conflito ao salvar os dados.';
    case 422:
      return 'Dados inválidos para processamento.';
    case 429:
      return 'Muitas requisições em sequência. Aguarde um instante e tente novamente.';
    default:
      return `Erro ${erro.status} ao falar com a API.`;
  }
}
