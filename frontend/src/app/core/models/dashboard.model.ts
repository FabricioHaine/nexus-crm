/** Indicadores gerais do dashboard. */
export interface DashboardResumo {
  totalClientes: number;
  totalOportunidades: number;
  totalOportunidadesAbertas: number;
  vendasGanhas: number;
  vendasPerdidas: number;
  valorTotalAbertas: number;
  valorTotalGanho: number;
  valorTotalPerdido: number;
  taxaConversao: number;
}

/** Distribuicao das oportunidades por etapa do funil. */
export interface OportunidadePorEtapa {
  etapa: string;
  etapaDescricao: string;
  quantidade: number;
  valorTotal: number;
}

/** Evolucao mensal das vendas ganhas. */
export interface VendasPorPeriodo {
  periodo: string;
  quantidade: number;
  valorTotal: number;
}
