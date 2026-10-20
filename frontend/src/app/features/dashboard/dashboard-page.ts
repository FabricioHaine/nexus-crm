import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';
import { Skeleton } from 'primeng/skeleton';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  DashboardResumo,
  OportunidadePorEtapa,
  VendasPorPeriodo,
} from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

interface CartaoIndicador {
  rotulo: string;
  valor: string;
  icone: string;
  tom: 'primario' | 'sucesso' | 'perigo' | 'neutro' | 'destaque';
  auxiliar?: string;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [
    UIChart,
    Skeleton,
    Button,
    RouterLink,
    NgFor,
    NgIf,
    CurrencyPipe,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly dashboardService = inject(DashboardService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly resumo = signal<DashboardResumo | null>(null);
  readonly porEtapa = signal<OportunidadePorEtapa[]>([]);
  readonly porPeriodo = signal<VendasPorPeriodo[]>([]);

  readonly cartoes = computed<CartaoIndicador[]>(() => {
    const dados = this.resumo();
    if (!dados) {
      return [];
    }
    return [
      {
        rotulo: 'Clientes cadastrados',
        valor: String(dados.totalClientes),
        icone: 'pi pi-users',
        tom: 'primario',
        auxiliar: `${dados.totalOportunidades} oportunidades`,
      },
      {
        rotulo: 'Oportunidades abertas',
        valor: String(dados.totalOportunidadesAbertas),
        icone: 'pi pi-briefcase',
        tom: 'destaque',
        auxiliar: this.formatarMoeda(dados.valorTotalAbertas),
      },
      {
        rotulo: 'Vendas ganhas',
        valor: String(dados.vendasGanhas),
        icone: 'pi pi-check-circle',
        tom: 'sucesso',
        auxiliar: this.formatarMoeda(dados.valorTotalGanho),
      },
      {
        rotulo: 'Vendas perdidas',
        valor: String(dados.vendasPerdidas),
        icone: 'pi pi-times-circle',
        tom: 'perigo',
        auxiliar: this.formatarMoeda(dados.valorTotalPerdido),
      },
      {
        rotulo: 'Taxa de conversao',
        valor: `${dados.taxaConversao.toFixed(1)}%`,
        icone: 'pi pi-chart-line',
        tom: 'neutro',
        auxiliar: 'ganhas / (ganhas + perdidas)',
      },
    ];
  });

  readonly dadosGraficoEtapas = computed(() => {
    const etapas = this.porEtapa();
    return {
      labels: etapas.map((etapa) => etapa.etapaDescricao),
      datasets: [
        {
          data: etapas.map((etapa) => etapa.quantidade),
          backgroundColor: ['#3b82f6', '#6366f1', '#f59e0b', '#16a34a', '#dc2626'],
          borderWidth: 0,
          hoverOffset: 6,
        },
      ],
    };
  });

  readonly opcoesGraficoEtapas = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: {
      legend: {
        position: 'right' as const,
        labels: { boxWidth: 12, font: { size: 12 }, usePointStyle: true },
      },
      tooltip: {
        callbacks: {
          label: (contexto: { dataset: { data: number[] }; label: string; parsed: number }) =>
            ` ${contexto.label}: ${contexto.parsed} oportunidade(s)`,
        },
      },
    },
  };

  readonly dadosGraficoEvolucao = computed(() => {
    const periodos = this.porPeriodo();
    return {
      labels: periodos.map((periodo) => this.rotularPeriodo(periodo.periodo)),
      datasets: [
        {
          label: 'Vendas ganhas (R$)',
          data: periodos.map((periodo) => periodo.valorTotal),
          backgroundColor: 'rgba(37, 99, 235, 0.12)',
          borderColor: '#2563eb',
          borderWidth: 2,
          fill: true,
          tension: 0.35,
          pointRadius: 3,
          pointBackgroundColor: '#2563eb',
        },
      ],
    };
  });

  readonly opcoesGraficoEvolucao = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (contexto: { parsed: { y: number } }) =>
            ` ${this.formatarMoeda(contexto.parsed.y)}`,
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (valor: number) =>
            new Intl.NumberFormat('pt-BR', {
              style: 'currency',
              currency: 'BRL',
              maximumFractionDigits: 0,
              notation: 'compact',
            }).format(valor),
        },
        grid: { color: '#eef1f6' },
      },
      x: { grid: { display: false } },
    },
  };

  readonly semDados = computed(() => {
    const dados = this.resumo();
    return !!dados && dados.totalOportunidades === 0;
  });

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const falhas: string[] = [];

    forkJoin({
      resumo: this.dashboardService.resumo().pipe(
        catchError((erro) => {
          falhas.push(mensagemErro(erro, 'Nao foi possivel carregar os indicadores do dashboard.'));
          return of(null);
        }),
      ),
      etapas: this.dashboardService.oportunidadesPorEtapa().pipe(
        catchError(() => {
          falhas.push('Nao foi possivel carregar a distribuicao por etapa.');
          return of([] as OportunidadePorEtapa[]);
        }),
      ),
      periodo: this.dashboardService.vendasPorPeriodo(6).pipe(
        catchError(() => {
          falhas.push('Nao foi possivel carregar a evolucao das vendas.');
          return of([] as VendasPorPeriodo[]);
        }),
      ),
    }).subscribe(({ resumo, etapas, periodo }) => {
      if (resumo) {
        this.resumo.set(resumo);
      }
      this.porEtapa.set(etapas);
      this.porPeriodo.set(periodo);
      this.erro.set(falhas.length > 0 ? falhas[0] : null);
      this.carregando.set(false);
    });
  }

  private formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      maximumFractionDigits: 0,
    }).format(valor);
  }

  private rotularPeriodo(periodo: string): string {
    const [ano, mes] = periodo.split('-');
    const meses = [
      'jan',
      'fev',
      'mar',
      'abr',
      'mai',
      'jun',
      'jul',
      'ago',
      'set',
      'out',
      'nov',
      'dez',
    ];
    return `${meses[Number(mes) - 1] ?? mes}/${String(ano).slice(2)}`;
  }
}
