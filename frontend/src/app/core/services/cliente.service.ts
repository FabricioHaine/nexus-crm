import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Cliente, ClientePayload, Pagina } from '../models/cliente.model';

/**
 * Comunicacao com a API de clientes do Nexus CRM.
 */
@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/clientes`;

  listar(
    termo: string | null,
    pagina: number,
    tamanho: number,
    ordenacao: string = 'nome,asc',
  ): Observable<Pagina<Cliente>> {
    let params = new HttpParams()
      .set('page', pagina)
      .set('size', tamanho)
      .set('sort', ordenacao);

    if (termo && termo.trim().length > 0) {
      params = params.set('termo', termo.trim());
    }

    return this.http.get<Pagina<Cliente>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.baseUrl}/${id}`);
  }

  criar(payload: ClientePayload): Observable<Cliente> {
    return this.http.post<Cliente>(this.baseUrl, payload);
  }

  atualizar(id: number, payload: ClientePayload): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
