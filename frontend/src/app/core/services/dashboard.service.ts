import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DashboardResumo,
  OportunidadePorEtapa,
  VendasPorPeriodo,
} from '../models/dashboard.model';

/**
 * Comunicacao com os endpoints de indicadores do dashboard.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  resumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(`${this.baseUrl}/resumo`);
  }

  oportunidadesPorEtapa(): Observable<OportunidadePorEtapa[]> {
    return this.http.get<OportunidadePorEtapa[]>(`${this.baseUrl}/oportunidades-por-etapa`);
  }

  vendasPorPeriodo(meses: number = 6): Observable<VendasPorPeriodo[]> {
    const params = new HttpParams().set('meses', meses);
    return this.http.get<VendasPorPeriodo[]>(`${this.baseUrl}/vendas-por-periodo`, { params });
  }
}
