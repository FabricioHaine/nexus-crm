import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AlterarEtapaPayload,
  EtapaOportunidade,
  Oportunidade,
  OportunidadePayload,
} from '../models/oportunidade.model';
import { Pagina } from '../models/cliente.model';

export interface FiltroOportunidades {
  clienteId?: number | null;
  etapa?: EtapaOportunidade | null;
  dataInicial?: string | null;
  dataFinal?: string | null;
}

/**
 * Comunicacao com a API de oportunidades comerciais do Nexus CRM.
 */
@Injectable({ providedIn: 'root' })
export class OportunidadeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/oportunidades`;

  listar(
    filtro: FiltroOportunidades,
    pagina: number,
    tamanho: number,
    ordenacao: string = 'dataCriacao,desc',
  ): Observable<Pagina<Oportunidade>> {
    let params = new HttpParams()
      .set('page', pagina)
      .set('size', tamanho)
      .set('sort', ordenacao);

    if (filtro.clienteId) {
      params = params.set('clienteId', filtro.clienteId);
    }
    if (filtro.etapa) {
      params = params.set('etapa', filtro.etapa);
    }
    if (filtro.dataInicial) {
      params = params.set('dataInicial', filtro.dataInicial);
    }
    if (filtro.dataFinal) {
      params = params.set('dataFinal', filtro.dataFinal);
    }

    return this.http.get<Pagina<Oportunidade>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Oportunidade> {
    return this.http.get<Oportunidade>(`${this.baseUrl}/${id}`);
  }

  criar(payload: OportunidadePayload): Observable<Oportunidade> {
    return this.http.post<Oportunidade>(this.baseUrl, payload);
  }

  atualizar(id: number, payload: OportunidadePayload): Observable<Oportunidade> {
    return this.http.put<Oportunidade>(`${this.baseUrl}/${id}`, payload);
  }

  alterarEtapa(id: number, payload: AlterarEtapaPayload): Observable<Oportunidade> {
    return this.http.patch<Oportunidade>(`${this.baseUrl}/${id}/etapa`, payload);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
