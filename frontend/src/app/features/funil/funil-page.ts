import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Skeleton } from 'primeng/skeleton';

import { DashboardResumo, OportunidadePorEtapa } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

interface EtapaFunil {
  etapa: string;
  rotulo: string;
  quantidade: number;
  valorTotal: number;
  percentual: number;
  conversaoDaAnterior: number | null;
}

@Component({
  selector: 'app-funil-page',
  imports: [Button, TableModule, Skeleton, NgFor, NgIf, CurrencyPipe],
  templateUrl: './funil-page.html',
  styleUrl: './funil-page.scss',
})
export class FunilPage {
  private readonly dashboardService = inject(DashboardService);

  readonly etapas = signal<EtapaFunil[]>([]);
  readonly resumo = signal<DashboardResumo | null>(null);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly maiorVolume = computed(() =>
    this.etapas().reduce((maior, etapa) => Math.max(maior, etapa.quantidade), 0),
  );

  readonly valorPipeline = computed(() =>
    this.etapas()
      .filter((etapa) => !['GANHO', 'PERDIDO'].includes(etapa.etapa))
      .reduce((total, etapa) => total + etapa.valorTotal, 0),
  );

  /** Valor total das vendas ganhas. */
  valorGanho(): number {
    return this.resumo()?.valorTotalGanho ?? 0;
  }

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.dashboardService.oportunidadesPorEtapa().subscribe({
      next: (etapas: OportunidadePorEtapa[]) => {
        const maior = etapas.reduce((max, etapa) => Math.max(max, etapa.quantidade), 0);
        const etapasMontadas = etapas.map((etapa, indice) => {
          const anterior = indice > 0 ? etapas[indice - 1].quantidade : null;
          return {
            etapa: etapa.etapa,
            rotulo: etapa.etapaDescricao,
            quantidade: etapa.quantidade,
            valorTotal: etapa.valorTotal,
            percentual: maior > 0 ? (etapa.quantidade / maior) * 100 : 0,
            conversaoDaAnterior:
              anterior !== null && anterior > 0
                ? (etapa.quantidade / anterior) * 100
                : null,
          };
        });
        this.etapas.set(etapasMontadas);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemErro(erro, 'Nao foi possivel carregar o funil comercial.'));
        this.carregando.set(false);
      },
    });

    this.dashboardService.resumo().subscribe({
      next: (resumo) => this.resumo.set(resumo),
      error: () => this.resumo.set(null),
    });
  }
}
