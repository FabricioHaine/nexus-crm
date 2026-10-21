import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Prepara as requisicoes enviadas a API.
 *
 * <ul>
 *   <li>Envia as credenciais da sessao (o cookie {@code HttpOnly} e
 *       anexado automaticamente);</li>
 *   <li>Quando a API responde 401, tenta renovar a sessao uma unica vez e
 *       repete a requisicao original.</li>
 * </ul>
 *
 * <p>O token de CSRF e Carecido pelo proprio {@code HttpClient} do Angular,
 * que copia o cookie {@code XSRF-TOKEN} para o cabecalho
 * {@code X-XSRF-TOKEN} nas requisicoes de escrita.</p>
 */
export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const authService = inject(AuthService);

  const requisicaoComCredenciais = requisicao.clone({ withCredentials: true });

  return proximo(requisicaoComCredenciais).pipe(
    catchError((erro) => {
      const ehFalhaDeSessao = erro instanceof HttpErrorResponse && erro.status === 401;
      const ehRotaDeAutenticacao = requisicao.url.includes('/auth/');

      if (!ehFalhaDeSessao || ehRotaDeAutenticacao) {
        return throwError(() => erro);
      }

      return authService.renovar().pipe(
        switchMap((usuario) =>
          usuario
            ? proximo(requisicaoComCredenciais)
            : throwError(() => erro),
        ),
        catchError((erroFinal) => {
          authService.logout();
          return throwError(() => erroFinal);
        }),
      );
    }),
  );
};
