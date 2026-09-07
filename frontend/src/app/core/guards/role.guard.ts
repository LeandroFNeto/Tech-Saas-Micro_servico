import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PapelUsuario } from '../../shared/models/auth.models';
import { AuthService } from '../services/auth.service';

export const roleGuard = (papel: PapelUsuario): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.possuiPapel(papel)) {
      return true;
    }

    if (auth.isAuthenticated()) {
      return router.createUrlTree([auth.rotaInicial()]);
    }

    return router.createUrlTree(['/login']);
  };
};
