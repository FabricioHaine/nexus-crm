import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Adiciona o token de acesso no cabecalho `Authorization` de toda requisicao
 * enviada a API.
 *
 * <p>Quando a API responde 401 (token expirado ou invalido), a sessao local e
 * encerrada para que o usuario volte a tela de login.</p>
 */
export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const authService = inject(AuthService);
  const token = authService.token();

  const requisicaoAutenticada = token
    ? requisicao.clone({ setHeaders: { Authorization: `${'Bearer'} ${token}` } })
    : requisicao;

  return proximo(requisicaoAutenticada).pipe(
    catchError((erro) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401) {
        authService.logout();
      }
      return throwError(() => erro);
    }),
  );
};
