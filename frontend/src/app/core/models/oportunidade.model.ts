/** Etapas do funil comercial aceitas pela API. */
export type EtapaOportunidade =
  | 'NOVO_CONTATO'
  | 'QUALIFICACAO'
  | 'PROPOSTA'
  | 'GANHO'
  | 'PERDIDO';

export interface Oportunidade {
  id: number;
  titulo: string;
  descricao: string | null;
  clienteId: number;
  clienteNome: string;
  clienteEmpresa: string | null;
  valorEstimado: number;
  etapa: EtapaOportunidade;
  etapaDescricao: string;
  probabilidade: number;
  dataCriacao: string;
  dataPrevistaFechamento: string | null;
  dataAtualizacao: string;
  dataFechamento: string | null;
  motivoPerda: string | null;
}

export interface OportunidadePayload {
  titulo: string;
  descricao?: string | null;
  clienteId: number;
  valorEstimado: number;
  etapa: EtapaOportunidade;
  probabilidade?: number | null;
  dataPrevistaFechamento?: string | null;
  motivoPerda?: string | null;
}

export interface AlterarEtapaPayload {
  etapa: EtapaOportunidade;
  motivoPerda?: string | null;
}

/** Rotulos e cores de cada etapa, usados nas tabelas e no funil. */
export interface EtapaInfo {
  valor: EtapaOportunidade;
  rotulo: string;
  severidade: 'info' | 'success' | 'warn' | 'danger' | 'secondary' | 'contrast';
  aberta: boolean;
  /** Probabilidade de conversao sugerida ao entrar na etapa. */
  probabilidadePadrao: number;
}

export const ETAPAS: EtapaInfo[] = [
  { valor: 'NOVO_CONTATO', rotulo: 'Novo contato', severidade: 'info', aberta: true, probabilidadePadrao: 10 },
  { valor: 'QUALIFICACAO', rotulo: 'Qualificacao', severidade: 'secondary', aberta: true, probabilidadePadrao: 35 },
  { valor: 'PROPOSTA', rotulo: 'Proposta', severidade: 'warn', aberta: true, probabilidadePadrao: 65 },
  { valor: 'GANHO', rotulo: 'Ganho', severidade: 'success', aberta: false, probabilidadePadrao: 100 },
  { valor: 'PERDIDO', rotulo: 'Perdido', severidade: 'danger', aberta: false, probabilidadePadrao: 0 },
];

export function etapaInfo(etapa: EtapaOportunidade): EtapaInfo {
  return ETAPAS.find((item) => item.valor === etapa) ?? ETAPAS[0];
}

/**
 * Etapas que podem ser alcancadas a partir da etapa atual.
 * Espelha a regra de negocio aplicada pela API, evitando oferecer transicoes invalidas.
 */
export function proximasEtapas(etapa: EtapaOportunidade): EtapaOportunidade[] {
  switch (etapa) {
    case 'NOVO_CONTATO':
      return ['QUALIFICACAO', 'PERDIDO'];
    case 'QUALIFICACAO':
      return ['PROPOSTA', 'PERDIDO'];
    case 'PROPOSTA':
      return ['GANHO', 'PERDIDO'];
    case 'GANHO':
    case 'PERDIDO':
      return [];
  }
}
