import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Usuario } from '../models/usuario.model';
import { AuthService } from '../services/auth.service';

/**
 * Protege as rotas internas: exige uma sessao ativa e, quando o perfil
 * necessario e informado, que o usuario o possua.
 */
export const authGuard: CanActivateFn = async (rota) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.garantirSessao();

  if (!usuario) {
    return router.createUrlTree(['/login'], { queryParams: { redirecionar: rota.url } });
  }

  const papeis = rota.data?.['papeis'] as Usuario['papel'][] | undefined;
  if (papeis && papeis.length > 0 && !papeis.includes(usuario.papel)) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};

/**
 * Impede que um usuario autenticado acesse a tela de login ou de cadastro.
 */
export const guestGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.garantirSessao();

  return usuario ? router.createUrlTree(['/dashboard']) : true;
};
