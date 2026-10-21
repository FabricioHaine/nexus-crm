import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { Usuario } from '../models/usuario.model';

/**
 * Protege as rotas internas: exige uma sessao ativa e, quando o perfil
 * necessario e informado, que o usuario o possua.
 */
export const authGuard: CanActivateFn = (rota) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.autenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { redirecionar: rota.url } });
  }

  const papeis = rota.data?.['papeis'] as Usuario['papel'][] | undefined;
  if (papeis && papeis.length > 0 && !authService.temPapel(...papeis)) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};

/**
 * Impede que um usuario autenticado acesse a tela de login ou de cadastro.
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.autenticado() ? router.createUrlTree(['/dashboard']) : true;
};
