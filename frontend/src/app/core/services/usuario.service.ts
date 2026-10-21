import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Papel, Usuario } from '../models/usuario.model';

/** Alteracoes administrativas sobre um usuario. */
export interface AlterarUsuarioPayload {
  papel?: Papel;
  ativo?: boolean;
}

/**
 * Comunicacao com a API de gestao de usuarios (restrita ao perfil ADMIN).
 */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.baseUrl);
  }

  alterar(id: number, payload: AlterarUsuarioPayload): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.baseUrl}/${id}`, payload);
  }
}
